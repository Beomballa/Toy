package com.section.admin.content.service;

import com.section.admin.content.res.ContentPerformanceAnalyticsResponse;
import com.section.admin.content.res.ContentPerformanceBulkAssignResponse;
import com.section.admin.content.res.ContentPerformanceBulkResolveResponse;
import com.section.admin.content.res.ContentPerformanceBulkTaskResponse;
import com.section.admin.content.res.ContentPerformanceTaskResponse;
import com.section.admin.log.service.AdminLogService;
import com.section.common.base.exception.BusinessException;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.system.entity.AdminOperationTask;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminOperationTaskRepository;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminContentPerformanceTaskServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-24T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private final AdminContentPerformanceAnalyticsService analyticsService =
            mock(AdminContentPerformanceAnalyticsService.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final AdminOperationTaskRepository taskRepository = mock(AdminOperationTaskRepository.class);
    private final AdminUserRepository adminUserRepository = mock(AdminUserRepository.class);
    private final AdminLogService adminLogService = mock(AdminLogService.class);
    private final AdminContentPerformanceTaskService service = new AdminContentPerformanceTaskService(
            analyticsService,
            documentRepository,
            taskRepository,
            adminUserRepository,
            adminLogService,
            FIXED_CLOCK
    );

    @Test
    @DisplayName("보완 필요 콘텐츠는 높은 우선순위의 출처 연결 작업으로 생성한다")
    void createsImprovementTask() {
        Document document = document(31L, Document.BoardType.STYLE, "여름 스타일");
        when(documentRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(document));
        when(taskRepository.findBySourceTypeAndSourceId("CONTENT_PERFORMANCE", 31L))
                .thenReturn(Optional.empty());
        when(analyticsService.getAnalytics(Document.BoardType.STYLE, 14))
                .thenReturn(analytics(content("IMPROVEMENT_REQUIRED", 84)));
        when(taskRepository.saveAndFlush(any(AdminOperationTask.class)))
                .thenReturn(AdminOperationTask.builder()
                        .taskNo(91L)
                        .status("TODO")
                        .priority("HIGH")
                        .dueDate(java.time.LocalDate.of(2026, 7, 27))
                        .sourceType("CONTENT_PERFORMANCE")
                        .sourceId(31L)
                        .build());

        ContentPerformanceTaskResponse response =
                service.createTask(31L, Document.BoardType.STYLE, 14);

        assertThat(response.created()).isTrue();
        assertThat(response.taskNo()).isEqualTo(91L);
        assertThat(response.priority()).isEqualTo("HIGH");
        assertThat(response.dueDate()).isEqualTo("2026-07-27");
        assertThat(response.taskPath()).contains("taskNo=91").contains("source=content-performance");

        ArgumentCaptor<AdminOperationTask> captor = ArgumentCaptor.forClass(AdminOperationTask.class);
        verify(taskRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("[콘텐츠 개선 #31] 여름 스타일");
        assertThat(captor.getValue().getDescription())
                .contains("조회수: 50회")
                .contains("우선순위 점수: 84점");
        assertThat(captor.getValue().getSourceType()).isEqualTo("CONTENT_PERFORMANCE");
        assertThat(captor.getValue().getSourceId()).isEqualTo(31L);
        verify(adminLogService).recordCurrentAdminLog("TASK_CREATE", 91L);
        verify(adminLogService).recordCurrentAdminLog("CONTENT_PERFORMANCE_TASK_CREATE", 31L);
    }

    @Test
    @DisplayName("이미 연결된 작업이 있으면 분석 재조회나 중복 저장 없이 기존 작업을 반환한다")
    void returnsExistingTask() {
        Document document = document(31L, Document.BoardType.STYLE, "여름 스타일");
        AdminOperationTask existing = AdminOperationTask.builder()
                .taskNo(77L)
                .status("IN_PROGRESS")
                .priority("HIGH")
                .sourceType("CONTENT_PERFORMANCE")
                .sourceId(31L)
                .build();
        when(documentRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(document));
        when(taskRepository.findBySourceTypeAndSourceId("CONTENT_PERFORMANCE", 31L))
                .thenReturn(Optional.of(existing));

        ContentPerformanceTaskResponse response =
                service.createTask(31L, Document.BoardType.STYLE, 7);

        assertThat(response.created()).isFalse();
        assertThat(response.taskNo()).isEqualTo(77L);
        assertThat(response.message()).contains("이미 연결");
        verify(analyticsService, never()).getAnalytics(any(), org.mockito.ArgumentMatchers.anyInt());
        verify(taskRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("반응 확보 콘텐츠는 문서 게시판을 사용해 보통 우선순위와 5일 기한으로 생성한다")
    void createsFeedbackTaskUsingDocumentBoard() {
        String longTitle = "반응 확보가 필요한 콘텐츠 ".repeat(15);
        Document document = document(42L, Document.BoardType.QNA, longTitle);
        when(documentRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(document));
        when(taskRepository.findBySourceTypeAndSourceId("CONTENT_PERFORMANCE", 42L))
                .thenReturn(Optional.empty());
        ContentPerformanceAnalyticsResponse.Content feedback = new ContentPerformanceAnalyticsResponse.Content(
                42L, "QNA", longTitle, 20, 12,
                0, 0, 0, 0, 0, 70, "FEEDBACK_NEEDED",
                "평가 노출을 점검해 주세요.", null, null
        );
        when(analyticsService.getAnalytics(Document.BoardType.QNA, 30))
                .thenReturn(new ContentPerformanceAnalyticsResponse(
                        "QNA", 30, "2026-06-25", "2026-07-24", "2026-07-24 12:00:00",
                        new ContentPerformanceAnalyticsResponse.Summary(20, 0, 0, 0, 1, 1, 0, 1),
                        List.of(feedback)
                ));
        when(taskRepository.saveAndFlush(any(AdminOperationTask.class)))
                .thenReturn(AdminOperationTask.builder()
                        .taskNo(92L)
                        .status("TODO")
                        .priority("MEDIUM")
                        .dueDate(java.time.LocalDate.of(2026, 7, 29))
                        .sourceType("CONTENT_PERFORMANCE")
                        .sourceId(42L)
                        .build());

        ContentPerformanceTaskResponse response = service.createTask(42L, null, 30);

        assertThat(response.priority()).isEqualTo("MEDIUM");
        assertThat(response.dueDate()).isEqualTo("2026-07-29");
        ArgumentCaptor<AdminOperationTask> captor = ArgumentCaptor.forClass(AdminOperationTask.class);
        verify(taskRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTitle()).hasSizeLessThanOrEqualTo(200);
        assertThat(captor.getValue().getIsPinned()).isEqualTo("N");
    }

    @Test
    @DisplayName("안정 상태 콘텐츠는 운영 작업으로 생성하지 않는다")
    void rejectsHealthyContent() {
        Document document = document(31L, Document.BoardType.NOTICE, "안정 공지");
        when(documentRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(document));
        when(taskRepository.findBySourceTypeAndSourceId("CONTENT_PERFORMANCE", 31L))
                .thenReturn(Optional.empty());
        when(analyticsService.getAnalytics(Document.BoardType.NOTICE, 7))
                .thenReturn(analytics(content("HEALTHY", 20)));

        assertThatThrownBy(() -> service.createTask(31L, Document.BoardType.NOTICE, 7))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("작업 생성이 필요하지 않습니다");
        verify(taskRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("요청 게시판이 실제 문서 게시판과 다르면 생성을 거부한다")
    void rejectsBoardMismatch() {
        when(documentRepository.findByIdForUpdate(31L))
                .thenReturn(Optional.of(document(31L, Document.BoardType.STYLE, "스타일")));

        assertThatThrownBy(() -> service.createTask(31L, Document.BoardType.NOTICE, 7))
                .isInstanceOf(BusinessException.class);
        verify(taskRepository, never()).findBySourceTypeAndSourceId(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠는 운영 작업 생성 전에 거부한다")
    void rejectsMissingDocument() {
        when(documentRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createTask(404L, null, 7))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("존재하지 않는 게시물");
        verify(taskRepository, never()).findBySourceTypeAndSourceId(any(), any());
    }

    @Test
    @DisplayName("일괄 생성은 한 번의 분석과 정렬 잠금으로 신규·기존·제외 결과를 집계한다")
    void createsBulkTasksWithSingleSnapshot() {
        ContentPerformanceAnalyticsResponse.Content third = content(
                3L, "STYLE", "세 번째", "IMPROVEMENT_REQUIRED", 90
        );
        ContentPerformanceAnalyticsResponse.Content second = content(
                2L, "STYLE", "두 번째", "FEEDBACK_NEEDED", 70
        );
        ContentPerformanceAnalyticsResponse.Content first = content(
                1L, "STYLE", "첫 번째", "IMPROVEMENT_REQUIRED", 60
        );
        when(analyticsService.getAnalytics(Document.BoardType.STYLE, 14))
                .thenReturn(new ContentPerformanceAnalyticsResponse(
                        "STYLE", 14, "2026-07-11", "2026-07-24", "2026-07-24 12:00:00",
                        new ContentPerformanceAnalyticsResponse.Summary(100, 5, 40, 5, 3, 3, 1, 2),
                        List.of(third, second, first)
                ));
        when(documentRepository.findAllByIdInForUpdate(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(
                        document(1L, Document.BoardType.STYLE, "첫 번째"),
                        document(2L, Document.BoardType.STYLE, "두 번째")
                ));
        AdminOperationTask existing = AdminOperationTask.builder()
                .taskNo(82L)
                .status("IN_PROGRESS")
                .priority("MEDIUM")
                .sourceType("CONTENT_PERFORMANCE")
                .sourceId(2L)
                .build();
        when(taskRepository.findAllBySourceTypeAndSourceIdIn(
                "CONTENT_PERFORMANCE",
                List.of(1L, 2L, 3L)
        )).thenReturn(List.of(existing));
        when(taskRepository.saveAllAndFlush(anyList()))
                .thenReturn(List.of(AdminOperationTask.builder()
                        .taskNo(81L)
                        .status("TODO")
                        .priority("HIGH")
                        .dueDate(java.time.LocalDate.of(2026, 7, 27))
                        .sourceType("CONTENT_PERFORMANCE")
                        .sourceId(1L)
                        .build()));

        ContentPerformanceBulkTaskResponse response =
                service.createTasks(Document.BoardType.STYLE, 14);

        assertThat(response.requestedCount()).isEqualTo(3);
        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.existingCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.tasks()).extracting(ContentPerformanceTaskResponse::taskNo)
                .containsExactly(82L, 81L);
        assertThat(response.message()).contains("신규 1건").contains("기존 연결 1건").contains("제외");
        verify(analyticsService).getAnalytics(Document.BoardType.STYLE, 14);
        verify(documentRepository).findAllByIdInForUpdate(List.of(1L, 2L, 3L));
        verify(taskRepository).saveAllAndFlush(anyList());
        verify(adminLogService).recordCurrentAdminLog("TASK_CREATE", 81L);
    }

    @Test
    @DisplayName("일괄 생성은 조치 대상이 없으면 잠금과 저장 없이 빈 결과를 반환한다")
    void returnsEmptyBulkResultWithoutWrites() {
        when(analyticsService.getAnalytics(Document.BoardType.NOTICE, 7))
                .thenReturn(new ContentPerformanceAnalyticsResponse(
                        "NOTICE", 7, "2026-07-18", "2026-07-24", "2026-07-24 12:00:00",
                        new ContentPerformanceAnalyticsResponse.Summary(10, 2, 100, 20, 1, 0, 0, 0),
                        List.of(content(1L, "NOTICE", "안정 공지", "HEALTHY", 10))
                ));

        ContentPerformanceBulkTaskResponse response =
                service.createTasks(Document.BoardType.NOTICE, 7);

        assertThat(response.requestedCount()).isZero();
        assertThat(response.tasks()).isEmpty();
        assertThat(response.message()).contains("조치 대상이 없습니다");
        verify(documentRepository, never()).findAllByIdInForUpdate(any());
        verify(taskRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    @DisplayName("성과 회복 일괄 완료는 작업 번호 순으로 잠그고 완료·기존 완료·제외를 구분한다")
    void resolvesRecoveredTasksWithOrderedLockAndRevalidation() {
        ContentPerformanceAnalyticsResponse.Content first = recoveredContent(1L, 103L);
        ContentPerformanceAnalyticsResponse.Content second = recoveredContent(2L, 101L);
        ContentPerformanceAnalyticsResponse.Content third = recoveredContent(3L, 102L);
        when(analyticsService.getAnalytics(Document.BoardType.NOTICE, 7))
                .thenReturn(new ContentPerformanceAnalyticsResponse(
                        "NOTICE", 7, "2026-07-18", "2026-07-24", "2026-07-24 12:00:00",
                        new ContentPerformanceAnalyticsResponse.Summary(
                                30, 9, 100, 30, 3, 0, 0, 0, 3, 0, 3
                        ),
                        List.of(first, second, third)
                ));
        AdminOperationTask completed = task(101L, 2L, "CONTENT_PERFORMANCE", "DONE");
        AdminOperationTask skipped = task(102L, 999L, "CONTENT_PERFORMANCE", "IN_PROGRESS");
        AdminOperationTask open = task(103L, 1L, "CONTENT_PERFORMANCE", "IN_PROGRESS");
        when(taskRepository.findAllByTaskNoInForUpdate(List.of(101L, 102L, 103L)))
                .thenReturn(List.of(completed, skipped, open));

        ContentPerformanceBulkResolveResponse response =
                service.resolveRecoveredTasks(Document.BoardType.NOTICE, 7);

        assertThat(response.requestedCount()).isEqualTo(3);
        assertThat(response.completedCount()).isEqualTo(1);
        assertThat(response.alreadyCompletedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.completedTaskNos()).containsExactly(103L);
        assertThat(open.getStatus()).isEqualTo("DONE");
        assertThat(skipped.getStatus()).isEqualTo("IN_PROGRESS");
        verify(taskRepository).findAllByTaskNoInForUpdate(List.of(101L, 102L, 103L));
        verify(taskRepository).flush();
        verify(adminLogService).recordCurrentAdminLog("TASK_STATUS_UPDATE", 103L);
        verify(adminLogService).recordCurrentAdminLog("CONTENT_PERFORMANCE_TASK_RESOLVE", 1L);
    }

    @Test
    @DisplayName("성과 회복 후보가 없으면 작업 잠금과 flush 없이 종료한다")
    void returnsEmptyResolveResultWithoutWrites() {
        when(analyticsService.getAnalytics(Document.BoardType.STYLE, 14))
                .thenReturn(new ContentPerformanceAnalyticsResponse(
                        "STYLE", 14, "2026-07-11", "2026-07-24", "2026-07-24 12:00:00",
                        new ContentPerformanceAnalyticsResponse.Summary(
                                10, 1, 100, 10, 1, 0, 0, 0, 0, 0, 0
                        ),
                        List.of(content(1L, "STYLE", "관찰", "LOW_SIGNAL", 10))
                ));

        ContentPerformanceBulkResolveResponse response =
                service.resolveRecoveredTasks(Document.BoardType.STYLE, 14);

        assertThat(response.requestedCount()).isZero();
        assertThat(response.completedTaskNos()).isEmpty();
        verify(taskRepository, never()).findAllByTaskNoInForUpdate(any());
        verify(taskRepository, never()).flush();
    }

    @Test
    @DisplayName("미배정 개선 작업은 추천 담당자의 현재 부하를 갱신하며 분산 배정한다")
    void assignsUnassignedTasksAcrossRecommendedAdmins() {
        List<ContentPerformanceAnalyticsResponse.Content> contents = List.of(
                unassignedContent(1L, 101L),
                unassignedContent(2L, 102L),
                unassignedContent(3L, 103L)
        );
        List<ContentPerformanceAnalyticsResponse.AssignmentRecommendation> recommendations = List.of(
                recommendation(10L, "운영 A", 0),
                recommendation(20L, "운영 B", 1)
        );
        when(analyticsService.getAnalytics(Document.BoardType.NOTICE, 7))
                .thenReturn(assignmentAnalytics(contents, recommendations));
        when(adminUserRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(
                activeAdmin(10L, "운영 A"),
                activeAdmin(20L, "운영 B")
        ));
        List<AdminOperationTask> tasks = List.of(
                task(101L, 1L, "CONTENT_PERFORMANCE", "TODO"),
                task(102L, 2L, "CONTENT_PERFORMANCE", "TODO"),
                task(103L, 3L, "CONTENT_PERFORMANCE", "TODO")
        );
        when(taskRepository.findAllByTaskNoInForUpdate(List.of(101L, 102L, 103L))).thenReturn(tasks);

        ContentPerformanceBulkAssignResponse response =
                service.assignUnassignedTasks(Document.BoardType.NOTICE, 7);

        assertThat(response.requestedCount()).isEqualTo(3);
        assertThat(response.assignedCount()).isEqualTo(3);
        assertThat(response.assignments())
                .extracting(ContentPerformanceBulkAssignResponse.Assignment::adminNo)
                .containsExactly(10L, 10L, 20L);
        assertThat(tasks).extracting(AdminOperationTask::getAssigneeAdminNo)
                .containsExactly(10L, 10L, 20L);
        verify(taskRepository).findAllByTaskNoInForUpdate(List.of(101L, 102L, 103L));
        verify(taskRepository).flush();
        verify(adminLogService).recordCurrentAdminLog("TASK_ASSIGN", 101L);
    }

    @Test
    @DisplayName("일괄 배정은 비활성 추천자를 제외하고 잠금 후 기존 배정과 출처 변경을 구분한다")
    void revalidatesAdminsAndTasksBeforeAssignment() {
        List<ContentPerformanceAnalyticsResponse.Content> contents = List.of(
                unassignedContent(1L, 101L),
                unassignedContent(2L, 102L),
                unassignedContent(3L, 103L)
        );
        List<ContentPerformanceAnalyticsResponse.AssignmentRecommendation> recommendations = List.of(
                recommendation(10L, "활성", 0),
                recommendation(20L, "정지", 0)
        );
        when(analyticsService.getAnalytics(Document.BoardType.STYLE, 14))
                .thenReturn(assignmentAnalytics(contents, recommendations));
        when(adminUserRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(
                activeAdmin(10L, "활성"),
                AdminUser.builder().adminNo(20L).name("정지").status("SUSPENDED").build()
        ));
        AdminOperationTask assignable = task(101L, 1L, "CONTENT_PERFORMANCE", "TODO");
        AdminOperationTask assigned = task(102L, 2L, "CONTENT_PERFORMANCE", "TODO");
        assigned.updateAssignee(99L);
        AdminOperationTask changed = task(103L, 999L, "CONTENT_PERFORMANCE", "TODO");
        when(taskRepository.findAllByTaskNoInForUpdate(List.of(101L, 102L, 103L)))
                .thenReturn(List.of(assignable, assigned, changed));

        ContentPerformanceBulkAssignResponse response =
                service.assignUnassignedTasks(Document.BoardType.STYLE, 14);

        assertThat(response.assignedCount()).isEqualTo(1);
        assertThat(response.alreadyAssignedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.assignments().get(0).adminName()).isEqualTo("활성");
        assertThat(assignable.getAssigneeAdminNo()).isEqualTo(10L);
        assertThat(assigned.getAssigneeAdminNo()).isEqualTo(99L);
    }

    @Test
    @DisplayName("활성 추천 담당자가 없으면 작업 잠금과 flush 없이 전체를 제외한다")
    void skipsAssignmentWhenNoActiveRecommendationExists() {
        List<ContentPerformanceAnalyticsResponse.Content> contents =
                List.of(unassignedContent(1L, 101L));
        List<ContentPerformanceAnalyticsResponse.AssignmentRecommendation> recommendations =
                List.of(recommendation(20L, "정지", 0));
        when(analyticsService.getAnalytics(Document.BoardType.NOTICE, 7))
                .thenReturn(assignmentAnalytics(contents, recommendations));
        when(adminUserRepository.findAllById(List.of(20L))).thenReturn(List.of(
                AdminUser.builder().adminNo(20L).name("정지").status("SUSPENDED").build()
        ));

        ContentPerformanceBulkAssignResponse response =
                service.assignUnassignedTasks(Document.BoardType.NOTICE, 7);

        assertThat(response.assignedCount()).isZero();
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.message()).contains("활성 관리자");
        verify(taskRepository, never()).findAllByTaskNoInForUpdate(any());
        verify(taskRepository, never()).flush();
    }

    private Document document(long id, Document.BoardType boardType, String title) {
        Document document = new Document();
        document.setId(id);
        document.setBoardType(boardType);
        document.setTitle(title);
        return document;
    }

    private ContentPerformanceAnalyticsResponse analytics(
            ContentPerformanceAnalyticsResponse.Content content
    ) {
        return new ContentPerformanceAnalyticsResponse(
                content.boardType(), 7, "2026-07-18", "2026-07-24", "2026-07-24 12:00:00",
                new ContentPerformanceAnalyticsResponse.Summary(50, 4, 25, 8, 1, 1, 0, 1),
                List.of(content)
        );
    }

    private ContentPerformanceAnalyticsResponse.Content content(String status, int score) {
        return content(31L, "STYLE", "여름 스타일", status, score);
    }

    private ContentPerformanceAnalyticsResponse.Content content(
            long documentId,
            String boardType,
            String title,
            String status,
            int score
    ) {
        return new ContentPerformanceAnalyticsResponse.Content(
                documentId, boardType, title, 50, 20,
                4, 1, 3, 25, 8, score, status,
                "본문 보완이 필요합니다.", null, null
        );
    }

    private ContentPerformanceAnalyticsResponse.Content recoveredContent(long documentId, long taskNo) {
        return new ContentPerformanceAnalyticsResponse.Content(
                documentId, "NOTICE", "회복 콘텐츠 " + documentId, 10, 8,
                3, 3, 0, 100, 30, 10, "HEALTHY",
                "조회와 독자 반응이 안정적입니다.", taskNo, "/admin/settings/tasks?taskNo=" + taskNo,
                "IN_PROGRESS", "진행중", "2026-07-23", true, true
        );
    }

    private AdminOperationTask task(long taskNo, long sourceId, String sourceType, String status) {
        return AdminOperationTask.builder()
                .taskNo(taskNo)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .status(status)
                .build();
    }

    private ContentPerformanceAnalyticsResponse.Content unassignedContent(long documentId, long taskNo) {
        return new ContentPerformanceAnalyticsResponse.Content(
                documentId, "NOTICE", "개선 콘텐츠 " + documentId, 20, 10,
                1, 0, 1, 0, 5, 70, "IMPROVEMENT_REQUIRED",
                "본문 보완이 필요합니다.", taskNo, "/admin/settings/tasks?taskNo=" + taskNo,
                "TODO", "대기", "2026-07-27", false, false, null
        );
    }

    private ContentPerformanceAnalyticsResponse.AssignmentRecommendation recommendation(
            long adminNo,
            String name,
            long totalCount
    ) {
        return new ContentPerformanceAnalyticsResponse.AssignmentRecommendation(
                adminNo, name, totalCount, 0, 0, "현재 부하 기준 추천"
        );
    }

    private ContentPerformanceAnalyticsResponse assignmentAnalytics(
            List<ContentPerformanceAnalyticsResponse.Content> contents,
            List<ContentPerformanceAnalyticsResponse.AssignmentRecommendation> recommendations
    ) {
        return new ContentPerformanceAnalyticsResponse(
                "NOTICE", 7, "2026-07-18", "2026-07-24", "2026-07-24 12:00:00",
                new ContentPerformanceAnalyticsResponse.Summary(
                        60, 3, 0, 5, contents.size(), contents.size(), contents.size(), 0,
                        contents.size(), 0, 0, contents.size()
                ),
                contents,
                recommendations
        );
    }

    private AdminUser activeAdmin(long adminNo, String name) {
        return AdminUser.builder()
                .adminNo(adminNo)
                .name(name)
                .status("ACTIVE")
                .build();
    }
}
