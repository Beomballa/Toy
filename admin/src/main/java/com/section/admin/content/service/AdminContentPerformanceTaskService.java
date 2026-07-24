package com.section.admin.content.service;

import com.section.admin.content.res.ContentPerformanceAnalyticsResponse;
import com.section.admin.content.res.ContentPerformanceBulkAssignResponse;
import com.section.admin.content.res.ContentPerformanceBulkResolveResponse;
import com.section.admin.content.res.ContentPerformanceBulkTaskResponse;
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
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminOperationTaskRepository;
import com.section.common.system.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminContentPerformanceTaskService {

    private static final int TITLE_MAX_LENGTH = 200;

    private final AdminContentPerformanceAnalyticsService analyticsService;
    private final DocumentRepository documentRepository;
    private final AdminOperationTaskRepository taskRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminLogService adminLogService;
    private final Clock clock;

    @Autowired
    public AdminContentPerformanceTaskService(
            AdminContentPerformanceAnalyticsService analyticsService,
            DocumentRepository documentRepository,
            AdminOperationTaskRepository taskRepository,
            AdminUserRepository adminUserRepository,
            AdminLogService adminLogService
    ) {
        this(
                analyticsService,
                documentRepository,
                taskRepository,
                adminUserRepository,
                adminLogService,
                Clock.systemDefaultZone()
        );
    }

    AdminContentPerformanceTaskService(
            AdminContentPerformanceAnalyticsService analyticsService,
            DocumentRepository documentRepository,
            AdminOperationTaskRepository taskRepository,
            AdminUserRepository adminUserRepository,
            AdminLogService adminLogService,
            Clock clock
    ) {
        this.analyticsService = analyticsService;
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.adminUserRepository = adminUserRepository;
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

    @Transactional
    public ContentPerformanceBulkTaskResponse createTasks(
            Document.BoardType boardType,
            int rangeDays
    ) {
        ContentPerformanceAnalyticsResponse analytics = analyticsService.getAnalytics(boardType, rangeDays);
        List<ContentPerformanceAnalyticsResponse.Content> insights = analytics.priorityContents().stream()
                .filter(item -> isActionRequired(item.status()))
                .toList();
        if (insights.isEmpty()) {
            return new ContentPerformanceBulkTaskResponse(
                    0, 0, 0, 0, List.of(), buildTaskListPath(boardType),
                    "현재 작업으로 전환할 조치 대상이 없습니다."
            );
        }

        List<Long> documentIds = insights.stream()
                .map(ContentPerformanceAnalyticsResponse.Content::documentId)
                .sorted()
                .toList();
        Map<Long, Document> documentsById = documentRepository.findAllByIdInForUpdate(documentIds).stream()
                .collect(Collectors.toMap(Document::getId, Function.identity()));
        Map<Long, AdminOperationTask> existingBySourceId = taskRepository
                .findAllBySourceTypeAndSourceIdIn(
                        AdminContentPerformanceAnalyticsService.CONTENT_PERFORMANCE_SOURCE_TYPE,
                        documentIds
                ).stream()
                .collect(Collectors.toMap(AdminOperationTask::getSourceId, Function.identity()));

        List<AdminOperationTask> createdTasks = insights.stream()
                .filter(insight -> documentsById.containsKey(insight.documentId()))
                .filter(insight -> !existingBySourceId.containsKey(insight.documentId()))
                .sorted(Comparator.comparingLong(ContentPerformanceAnalyticsResponse.Content::documentId))
                .map(insight -> buildTask(documentsById.get(insight.documentId()), insight))
                .toList();
        List<AdminOperationTask> savedTasks = createdTasks.isEmpty()
                ? List.of()
                : taskRepository.saveAllAndFlush(createdTasks);
        Map<Long, AdminOperationTask> createdBySourceId = savedTasks.stream()
                .collect(Collectors.toMap(AdminOperationTask::getSourceId, Function.identity()));
        savedTasks.forEach(task -> {
            adminLogService.recordCurrentAdminLog("TASK_CREATE", task.getTaskNo());
            adminLogService.recordCurrentAdminLog("CONTENT_PERFORMANCE_TASK_CREATE", task.getSourceId());
        });

        List<ContentPerformanceTaskResponse> taskResponses = insights.stream()
                .map(insight -> {
                    Document document = documentsById.get(insight.documentId());
                    if (document == null) {
                        return null;
                    }
                    AdminOperationTask existing = existingBySourceId.get(insight.documentId());
                    if (existing != null) {
                        return toResponse(existing, false, resolveBoardType(boardType, document));
                    }
                    AdminOperationTask created = createdBySourceId.get(insight.documentId());
                    return created == null
                            ? null
                            : toResponse(created, true, resolveBoardType(boardType, document));
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        int skippedCount = insights.size() - taskResponses.size();
        int existingCount = (int) taskResponses.stream().filter(task -> !task.created()).count();
        return new ContentPerformanceBulkTaskResponse(
                insights.size(),
                createdBySourceId.size(),
                existingCount,
                skippedCount,
                taskResponses,
                buildTaskListPath(boardType),
                buildBulkMessage(createdBySourceId.size(), existingCount, skippedCount)
        );
    }

    @Transactional
    public ContentPerformanceBulkResolveResponse resolveRecoveredTasks(
            Document.BoardType boardType,
            int rangeDays
    ) {
        ContentPerformanceAnalyticsResponse analytics = analyticsService.getAnalytics(boardType, rangeDays);
        List<ContentPerformanceAnalyticsResponse.Content> recovered = analytics.priorityContents().stream()
                .filter(ContentPerformanceAnalyticsResponse.Content::operationTaskRecoverable)
                .toList();
        if (recovered.isEmpty()) {
            return new ContentPerformanceBulkResolveResponse(
                    0, 0, 0, 0, List.of(), buildTaskListPath(boardType),
                    "현재 성과 회복으로 완료할 운영 작업이 없습니다."
            );
        }

        List<Long> taskNos = recovered.stream()
                .map(ContentPerformanceAnalyticsResponse.Content::operationTaskNo)
                .sorted()
                .toList();
        Map<Long, Long> expectedSourceIds = recovered.stream()
                .collect(Collectors.toMap(
                        ContentPerformanceAnalyticsResponse.Content::operationTaskNo,
                        ContentPerformanceAnalyticsResponse.Content::documentId
                ));
        List<AdminOperationTask> lockedTasks = taskRepository.findAllByTaskNoInForUpdate(taskNos);
        int alreadyCompletedCount = 0;
        int skippedCount = taskNos.size() - lockedTasks.size();
        List<AdminOperationTask> completedTasks = new ArrayList<>();
        for (AdminOperationTask task : lockedTasks) {
            if (AdminOperationTaskStatus.DONE.name().equals(task.getStatus())) {
                alreadyCompletedCount++;
                continue;
            }
            Long expectedSourceId = expectedSourceIds.get(task.getTaskNo());
            if (!AdminContentPerformanceAnalyticsService.CONTENT_PERFORMANCE_SOURCE_TYPE.equals(task.getSourceType())
                    || expectedSourceId == null
                    || !expectedSourceId.equals(task.getSourceId())) {
                skippedCount++;
                continue;
            }
            task.updateStatus(AdminOperationTaskStatus.DONE.name());
            completedTasks.add(task);
        }
        if (!completedTasks.isEmpty()) {
            taskRepository.flush();
        }
        completedTasks.forEach(task -> {
            adminLogService.recordCurrentAdminLog("TASK_STATUS_UPDATE", task.getTaskNo());
            adminLogService.recordCurrentAdminLog("CONTENT_PERFORMANCE_TASK_RESOLVE", task.getSourceId());
        });
        return new ContentPerformanceBulkResolveResponse(
                recovered.size(),
                completedTasks.size(),
                alreadyCompletedCount,
                skippedCount,
                completedTasks.stream().map(AdminOperationTask::getTaskNo).toList(),
                buildTaskListPath(boardType),
                buildResolveMessage(completedTasks.size(), alreadyCompletedCount, skippedCount)
        );
    }

    @Transactional
    public ContentPerformanceBulkAssignResponse assignUnassignedTasks(
            Document.BoardType boardType,
            int rangeDays
    ) {
        ContentPerformanceAnalyticsResponse analytics = analyticsService.getAnalytics(boardType, rangeDays);
        List<ContentPerformanceAnalyticsResponse.Content> unassigned = analytics.priorityContents().stream()
                .filter(item -> item.operationTaskNo() != null)
                .filter(item -> !AdminOperationTaskStatus.DONE.name().equals(item.operationTaskStatus()))
                .filter(item -> item.operationTaskAssigneeAdminNo() == null)
                .filter(item -> !item.operationTaskRecoverable())
                .toList();
        if (unassigned.isEmpty()) {
            return emptyAssignmentResponse(boardType, "현재 배정할 콘텐츠 개선 작업이 없습니다.");
        }

        List<Long> recommendedAdminNos = analytics.assignmentRecommendations().stream()
                .map(ContentPerformanceAnalyticsResponse.AssignmentRecommendation::adminNo)
                .toList();
        Map<Long, AdminUser> activeAdmins = adminUserRepository.findAllById(recommendedAdminNos).stream()
                .filter(admin -> "ACTIVE".equals(admin.getStatus()))
                .collect(Collectors.toMap(AdminUser::getAdminNo, Function.identity()));
        List<AssignmentCandidate> candidates = analytics.assignmentRecommendations().stream()
                .filter(item -> activeAdmins.containsKey(item.adminNo()))
                .map(AssignmentCandidate::new)
                .toList();
        if (candidates.isEmpty()) {
            return new ContentPerformanceBulkAssignResponse(
                    unassigned.size(), 0, 0, unassigned.size(), List.of(), buildTaskListPath(boardType),
                    "배정 가능한 활성 관리자가 없습니다."
            );
        }

        Map<Long, ContentPerformanceAnalyticsResponse.Content> expectedByTaskNo = unassigned.stream()
                .collect(Collectors.toMap(
                        ContentPerformanceAnalyticsResponse.Content::operationTaskNo,
                        Function.identity()
                ));
        List<Long> taskNos = expectedByTaskNo.keySet().stream().sorted().toList();
        List<AdminOperationTask> lockedTasks = taskRepository.findAllByTaskNoInForUpdate(taskNos);
        int alreadyAssignedCount = 0;
        int skippedCount = taskNos.size() - lockedTasks.size();
        List<ContentPerformanceBulkAssignResponse.Assignment> assignments = new ArrayList<>();
        for (AdminOperationTask task : lockedTasks) {
            ContentPerformanceAnalyticsResponse.Content expected = expectedByTaskNo.get(task.getTaskNo());
            if (AdminOperationTaskStatus.DONE.name().equals(task.getStatus())
                    || expected == null
                    || !AdminContentPerformanceAnalyticsService.CONTENT_PERFORMANCE_SOURCE_TYPE.equals(task.getSourceType())
                    || !Long.valueOf(expected.documentId()).equals(task.getSourceId())) {
                skippedCount++;
                continue;
            }
            if (task.getAssigneeAdminNo() != null) {
                alreadyAssignedCount++;
                continue;
            }
            AssignmentCandidate selected = candidates.stream()
                    .min(AssignmentCandidate.ORDER)
                    .orElseThrow();
            task.updateAssignee(selected.adminNo);
            selected.totalCount++;
            assignments.add(new ContentPerformanceBulkAssignResponse.Assignment(
                    task.getTaskNo(),
                    selected.adminNo,
                    activeAdmins.get(selected.adminNo).getName()
            ));
        }
        if (!assignments.isEmpty()) {
            taskRepository.flush();
        }
        assignments.forEach(assignment -> {
            adminLogService.recordCurrentAdminLog("TASK_ASSIGN", assignment.taskNo());
            adminLogService.recordCurrentAdminLog("CONTENT_PERFORMANCE_TASK_ASSIGN", assignment.taskNo());
        });
        return new ContentPerformanceBulkAssignResponse(
                unassigned.size(),
                assignments.size(),
                alreadyAssignedCount,
                skippedCount,
                assignments,
                buildTaskListPath(boardType),
                buildAssignmentMessage(assignments.size(), alreadyAssignedCount, skippedCount)
        );
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

        AdminOperationTask task = taskRepository.saveAndFlush(buildTask(document, insight));
        adminLogService.recordCurrentAdminLog("TASK_CREATE", task.getTaskNo());
        adminLogService.recordCurrentAdminLog("CONTENT_PERFORMANCE_TASK_CREATE", document.getId());
        return toResponse(task, true, boardType);
    }

    private AdminOperationTask buildTask(
            Document document,
            ContentPerformanceAnalyticsResponse.Content insight
    ) {
        LocalDate today = LocalDate.now(clock);
        String priority = "IMPROVEMENT_REQUIRED".equals(insight.status())
                ? AdminOperationTaskPriority.HIGH.name()
                : AdminOperationTaskPriority.MEDIUM.name();
        return AdminOperationTask.builder()
                .title(buildTitle(document))
                .description(buildDescription(insight))
                .status(AdminOperationTaskStatus.TODO.name())
                .priority(priority)
                .dueDate(today.plusDays("IMPROVEMENT_REQUIRED".equals(insight.status()) ? 3 : 5))
                .isPinned("HIGH".equals(priority) ? "Y" : "N")
                .sourceType(AdminContentPerformanceAnalyticsService.CONTENT_PERFORMANCE_SOURCE_TYPE)
                .sourceId(document.getId())
                .build();
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

    private Document.BoardType resolveBoardType(Document.BoardType requestedBoardType, Document document) {
        return requestedBoardType != null ? requestedBoardType : document.getBoardType();
    }

    private String buildTaskListPath(Document.BoardType boardType) {
        String returnTo = boardType == null
                ? "/admin/content/list"
                : "/admin/content/list?boardType=" + boardType.name();
        return "/admin/settings/tasks?source=content-performance-bulk&returnTo="
                + URLEncoder.encode(returnTo, StandardCharsets.UTF_8);
    }

    private String buildBulkMessage(int createdCount, int existingCount, int skippedCount) {
        String message = "신규 " + createdCount + "건, 기존 연결 " + existingCount + "건을 확인했습니다.";
        return skippedCount == 0 ? message : message + " 문서 변경으로 " + skippedCount + "건은 제외했습니다.";
    }

    private String buildResolveMessage(int completedCount, int alreadyCompletedCount, int skippedCount) {
        String message = "성과 회복 작업 " + completedCount + "건을 완료했습니다.";
        if (alreadyCompletedCount > 0) {
            message += " 이미 완료된 " + alreadyCompletedCount + "건을 확인했습니다.";
        }
        return skippedCount == 0 ? message : message + " 상태 변경으로 " + skippedCount + "건은 제외했습니다.";
    }

    private ContentPerformanceBulkAssignResponse emptyAssignmentResponse(
            Document.BoardType boardType,
            String message
    ) {
        return new ContentPerformanceBulkAssignResponse(
                0, 0, 0, 0, List.of(), buildTaskListPath(boardType), message
        );
    }

    private String buildAssignmentMessage(int assignedCount, int alreadyAssignedCount, int skippedCount) {
        String message = "콘텐츠 개선 작업 " + assignedCount + "건을 추천 담당자에게 배정했습니다.";
        if (alreadyAssignedCount > 0) {
            message += " 이미 배정된 " + alreadyAssignedCount + "건을 확인했습니다.";
        }
        return skippedCount == 0 ? message : message + " 상태 변경으로 " + skippedCount + "건은 제외했습니다.";
    }

    private static final class AssignmentCandidate {
        private static final Comparator<AssignmentCandidate> ORDER = Comparator
                .comparingLong((AssignmentCandidate item) -> item.overdueCount)
                .thenComparingLong(item -> item.inProgressCount)
                .thenComparingLong(item -> item.totalCount)
                .thenComparingLong(item -> item.adminNo);

        private final long adminNo;
        private final long inProgressCount;
        private final long overdueCount;
        private long totalCount;

        private AssignmentCandidate(ContentPerformanceAnalyticsResponse.AssignmentRecommendation recommendation) {
            this.adminNo = recommendation.adminNo();
            this.totalCount = recommendation.totalCount();
            this.inProgressCount = recommendation.inProgressCount();
            this.overdueCount = recommendation.overdueCount();
        }
    }
}
