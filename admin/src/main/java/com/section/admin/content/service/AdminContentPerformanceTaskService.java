package com.section.admin.content.service;

import com.section.admin.content.res.ContentPerformanceAnalyticsResponse;
import com.section.admin.content.res.ContentPerformanceTaskResponse;
import com.section.admin.log.service.AdminLogService;
import com.section.admin.task.support.AdminTaskLinkSupport;
import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.system.entity.AdminOperationTask;
import com.section.common.system.repository.AdminOperationTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class AdminContentPerformanceTaskService {

    private static final int TITLE_MAX_LENGTH = 200;

    private final AdminContentPerformanceAnalyticsService analyticsService;
    private final DocumentRepository documentRepository;
    private final AdminOperationTaskRepository taskRepository;
    private final AdminLogService adminLogService;
    private final Clock clock;

    @Autowired
    public AdminContentPerformanceTaskService(
            AdminContentPerformanceAnalyticsService analyticsService,
            DocumentRepository documentRepository,
            AdminOperationTaskRepository taskRepository,
            AdminLogService adminLogService
    ) {
        this(analyticsService, documentRepository, taskRepository, adminLogService, Clock.systemDefaultZone());
    }

    AdminContentPerformanceTaskService(
            AdminContentPerformanceAnalyticsService analyticsService,
            DocumentRepository documentRepository,
            AdminOperationTaskRepository taskRepository,
            AdminLogService adminLogService,
            Clock clock
    ) {
        this.analyticsService = analyticsService;
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.adminLogService = adminLogService;
        this.clock = clock;
    }

    @Transactional
    public ContentPerformanceTaskResponse createTask(
            long documentId,
            Document.BoardType requestedBoardType,
            int rangeDays
    ) {
        // 문서 단위 잠금으로 동시에 눌린 생성 요청도 같은 출처 작업 하나로 직렬화합니다.
        Document document = documentRepository.findByIdForUpdate(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
        Document.BoardType boardType = requestedBoardType == null ? document.getBoardType() : requestedBoardType;
        if (document.getBoardType() != boardType) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return taskRepository.findBySourceTypeAndSourceId(
                        AdminContentPerformanceAnalyticsService.CONTENT_PERFORMANCE_SOURCE_TYPE,
                        documentId
                )
                .map(task -> toResponse(task, false, boardType))
                .orElseGet(() -> createAnalyzedTask(document, boardType, rangeDays));
    }

    private ContentPerformanceTaskResponse createAnalyzedTask(
            Document document,
            Document.BoardType boardType,
            int rangeDays
    ) {
        ContentPerformanceAnalyticsResponse.Content insight = analyticsService.getAnalytics(boardType, rangeDays)
                .priorityContents()
                .stream()
                .filter(item -> item.documentId() == document.getId())
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "현재 효과 분석 우선순위에서 확인되지 않는 콘텐츠입니다.",
                        ErrorCode.INVALID_INPUT_VALUE
                ));
        if (!isActionRequired(insight.status())) {
            throw new BusinessException(
                    "현재 상태에서는 운영 작업 생성이 필요하지 않습니다.",
                    ErrorCode.INVALID_INPUT_VALUE
            );
        }

        LocalDate today = LocalDate.now(clock);
        String priority = "IMPROVEMENT_REQUIRED".equals(insight.status())
                ? AdminOperationTaskPriority.HIGH.name()
                : AdminOperationTaskPriority.MEDIUM.name();
        LocalDate dueDate = today.plusDays("IMPROVEMENT_REQUIRED".equals(insight.status()) ? 3 : 5);
        AdminOperationTask task = taskRepository.saveAndFlush(AdminOperationTask.builder()
                .title(buildTitle(document))
                .description(buildDescription(insight))
                .status(AdminOperationTaskStatus.TODO.name())
                .priority(priority)
                .dueDate(dueDate)
                .isPinned("HIGH".equals(priority) ? "Y" : "N")
                .sourceType(AdminContentPerformanceAnalyticsService.CONTENT_PERFORMANCE_SOURCE_TYPE)
                .sourceId(document.getId())
                .build());
        adminLogService.recordCurrentAdminLog("TASK_CREATE", task.getTaskNo());
        adminLogService.recordCurrentAdminLog("CONTENT_PERFORMANCE_TASK_CREATE", document.getId());
        return toResponse(task, true, boardType);
    }

    private boolean isActionRequired(String status) {
        return "IMPROVEMENT_REQUIRED".equals(status) || "FEEDBACK_NEEDED".equals(status);
    }

    private String buildTitle(Document document) {
        String prefix = "[콘텐츠 개선 #" + document.getId() + "] ";
        String title = document.getTitle() == null || document.getTitle().isBlank()
                ? "제목 없음"
                : document.getTitle().trim();
        int availableLength = TITLE_MAX_LENGTH - prefix.length();
        return prefix + (title.length() <= availableLength ? title : title.substring(0, availableLength).trim());
    }

    private String buildDescription(ContentPerformanceAnalyticsResponse.Content insight) {
        return """
                관리자 콘텐츠 효과 분석에서 생성된 운영 작업입니다.

                상태: %s
                판단 근거: %s
                조회수: %d회 / 순방문자: %d명
                반응수: %d건 / 도움 비율: %d%% / 반응 확보율: %d%%
                우선순위 점수: %d점
                콘텐츠 경로: /admin/content/get?id=%d&boardType=%s
                """.formatted(
                insight.status(),
                insight.statusMessage(),
                insight.viewCount(),
                insight.uniqueVisitors(),
                insight.reactionCount(),
                insight.helpfulRate(),
                insight.reactionCoverageRate(),
                insight.priorityScore(),
                insight.documentId(),
                insight.boardType()
        ).trim();
    }

    private ContentPerformanceTaskResponse toResponse(
            AdminOperationTask task,
            boolean created,
            Document.BoardType boardType
    ) {
        String returnTo = "/admin/content/list?boardType=" + boardType.name();
        return new ContentPerformanceTaskResponse(
                task.getTaskNo(),
                created,
                task.getStatus(),
                task.getPriority(),
                task.getDueDate() == null ? null : task.getDueDate().toString(),
                AdminTaskLinkSupport.buildListOpenPath(
                        task.getTaskNo(),
                        returnTo,
                        "content-performance"
                ),
                created ? "운영 작업을 생성했습니다." : "이미 연결된 운영 작업이 있습니다."
        );
    }
}
