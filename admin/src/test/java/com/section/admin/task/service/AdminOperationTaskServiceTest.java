package com.section.admin.task.service;

import com.section.admin.log.req.AdminLogListRequest;
import com.section.admin.log.res.AdminLogListResponse;
import com.section.admin.log.service.AdminLogService;
import com.section.admin.task.req.AdminOperationTaskBulkDeleteRequest;
import com.section.admin.task.req.AdminOperationTaskBulkOperateRequest;
import com.section.admin.task.req.AdminOperationTaskCommentSaveRequest;
import com.section.admin.task.req.AdminOperationTaskListRequest;
import com.section.admin.task.req.AdminOperationTaskSaveRequest;
import com.section.admin.task.res.AdminOperationTaskDetailResponse;
import com.section.admin.task.res.AdminOperationTaskListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.system.dto.AdminOperationTaskAssigneeRecommendationDto;
import com.section.common.system.dto.AdminOperationTaskCommentCountDto;
import com.section.common.system.dto.AdminOperationTaskListQuery;
import com.section.common.system.dto.AdminOperationTaskCommentResDto;
import com.section.common.system.dto.AdminOperationTaskCommentSummaryDto;
import com.section.common.system.dto.AdminOperationTaskListResDto;
import com.section.common.system.dto.AdminOperationTaskSummaryDto;
import com.section.common.system.entity.AdminOperationTask;
import com.section.common.system.entity.AdminOperationTaskComment;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminOperationTaskCommentRepository;
import com.section.common.system.repository.AdminOperationTaskRepository;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AdminOperationTaskServiceTest {

    @Mock
    private AdminOperationTaskRepository adminOperationTaskRepository;
    @Mock
    private AdminOperationTaskCommentRepository adminOperationTaskCommentRepository;
    @Mock
    private AdminUserRepository adminUserRepository;
    @Mock
    private AdminLogService adminLogService;

    @InjectMocks
    private AdminOperationTaskService adminOperationTaskService;

    @Test
    @DisplayName("운영 작업 목록은 페이지 응답과 통계를 반환한다")
    void getTaskListReturnsPagedResponse() {
        AdminOperationTaskListRequest request = new AdminOperationTaskListRequest();
        request.setKeyword("정리");
        request.setDueDateFrom(LocalDate.of(2026, 6, 1));
        request.setDueDateTo(LocalDate.of(2026, 6, 30));

        AdminOperationTaskListResDto row = new AdminOperationTaskListResDto();
        row.setTaskNo(1L);
        row.setTitle("정산 확인");
        row.setDescription("정산 마감 확인");
        row.setStatus("TODO");
        row.setPriority("HIGH");
        row.setAssigneeAdminNo(2L);
        row.setAssigneeAdminName("운영자");
        row.setDueDate(LocalDate.now());
        row.setIsPinned("Y");
        row.setCrtDtm(LocalDateTime.now());

        when(adminOperationTaskRepository.getTaskList(any(AdminOperationTaskListQuery.class), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1));
        when(adminOperationTaskRepository.getTaskSummary(any(AdminOperationTaskListQuery.class), any()))
                .thenReturn(new AdminOperationTaskSummaryDto(5, 2, 2, 1, 1));
        when(adminUserRepository.findAll()).thenReturn(List.of(
                AdminUser.builder().adminNo(2L).name("운영자").loginId("ops").password("pw").build()
        ));
        AdminOperationTaskCommentSummaryDto latestComment = new AdminOperationTaskCommentSummaryDto();
        latestComment.setTaskNo(1L);
        latestComment.setAdminName("운영자");
        latestComment.setContent("우선 확인이 필요한 작업입니다.");
        latestComment.setCrtDtm(LocalDateTime.of(2026, 6, 1, 8, 0));
        AdminOperationTaskCommentCountDto commentCount = new AdminOperationTaskCommentCountDto();
        commentCount.setTaskNo(1L);
        commentCount.setCommentCount(3L);
        when(adminOperationTaskCommentRepository.getLatestCommentsByTaskNos(List.of(1L))).thenReturn(List.of(latestComment));
        when(adminOperationTaskCommentRepository.getCommentCountsByTaskNos(List.of(1L))).thenReturn(List.of(commentCount));

        AdminOperationTaskListResponse response = adminOperationTaskService.getTaskList(request);

        assertEquals(1, response.items().size());
        assertEquals(1L, response.totalElements());
        assertEquals(5L, response.taskStats().totalCount());
        assertEquals("운영자", response.assigneeOptions().get(0).name());
        assertEquals(3L, response.items().get(0).commentCount());
        assertEquals("운영자 · 2026-06-01 08:00", response.items().get(0).latestCommentMeta());
        verify(adminOperationTaskRepository).getTaskList(
                argThat(query -> LocalDate.of(2026, 6, 1).equals(query.dueDateFrom())
                        && LocalDate.of(2026, 6, 30).equals(query.dueDateTo())),
                any()
        );
    }

    @Test
    @DisplayName("운영 작업 CSV 내보내기는 필터 결과를 기반으로 파일 바이트를 만든다")
    void exportTaskListCsvReturnsCsvBytes() {
        AdminOperationTaskListRequest request = new AdminOperationTaskListRequest();
        request.setStatus("TODO");
        request.setAssigneeAdminNo(2L);
        request.setCommentedOnly("Y");
        request.setSortBy("PRIORITY_DESC");
        request.setDueDateFrom(LocalDate.of(2026, 6, 1));
        request.setDueDateTo(LocalDate.of(2026, 6, 30));

        AdminOperationTaskListResDto row = new AdminOperationTaskListResDto();
        row.setTaskNo(1L);
        row.setTitle("정산 확인");
        row.setDescription("정산 마감 확인");
        row.setStatus("TODO");
        row.setPriority("HIGH");
        row.setAssigneeAdminNo(2L);
        row.setAssigneeAdminName("운영자");
        row.setDueDate(LocalDate.now());
        row.setIsPinned("Y");
        row.setCrtDtm(LocalDateTime.of(2026, 6, 1, 9, 0));

        when(adminOperationTaskRepository.getTaskList(any(AdminOperationTaskListQuery.class), eq(PageRequest.of(0, 1000))))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 1000), 1));
        when(adminUserRepository.findAllById(List.of(2L))).thenReturn(List.of(
                AdminUser.builder().adminNo(2L).name("운영자").loginId("ops").password("pw").build()
        ));
        AdminOperationTaskCommentSummaryDto latestComment = new AdminOperationTaskCommentSummaryDto();
        latestComment.setTaskNo(1L);
        latestComment.setAdminName("운영자");
        latestComment.setContent("최근 메모입니다.");
        latestComment.setCrtDtm(LocalDateTime.of(2026, 6, 1, 8, 30));
        AdminOperationTaskCommentCountDto commentCount = new AdminOperationTaskCommentCountDto();
        commentCount.setTaskNo(1L);
        commentCount.setCommentCount(2L);
        when(adminOperationTaskCommentRepository.getLatestCommentsByTaskNos(List.of(1L))).thenReturn(List.of(latestComment));
        when(adminOperationTaskCommentRepository.getCommentCountsByTaskNos(List.of(1L))).thenReturn(List.of(commentCount));

        byte[] result = adminOperationTaskService.exportTaskListCsv(request);
        String csv = new String(result, java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"조회조건\",\"상태: 대기 | 담당자: 운영자 | 메모있는 작업만 | 기한: 2026-06-01 ~ 2026-06-30\""));
        assertTrue(csv.contains("\"정렬\",\"우선순위 높은 순\""));
        assertTrue(csv.contains("메모있는 작업만"));
        assertTrue(csv.contains("\"정산 확인\""));
        assertTrue(csv.contains("\"2\",\"최근 메모입니다.\",\"운영자\",\"2026-06-01 08:30\""));
    }

    @Test
    @DisplayName("신규 운영 작업 저장은 활동 로그를 남긴다")
    void saveTaskCreatesEntity() {
        when(adminOperationTaskRepository.save(any(AdminOperationTask.class)))
                .thenAnswer(invocation -> {
                    AdminOperationTask entity = invocation.getArgument(0);
                    try {
                        java.lang.reflect.Field taskNoField = AdminOperationTask.class.getDeclaredField("taskNo");
                        taskNoField.setAccessible(true);
                        taskNoField.set(entity, 15L);
                    } catch (ReflectiveOperationException exception) {
                        throw new RuntimeException(exception);
                    }
                    return entity;
                });

        adminOperationTaskService.saveTask(new AdminOperationTaskSaveRequest(
                null,
                "  재고 점검  ",
                "  주간  재고 정리  ",
                "TODO",
                "HIGH",
                null,
                LocalDate.now().plusDays(1),
                "Y"
        ));

        verify(adminOperationTaskRepository).save(any(AdminOperationTask.class));
        verify(adminLogService).recordCurrentAdminLog("TASK_CREATE", 15L);
    }

    @Test
    @DisplayName("기존 운영 작업 수정은 담당자 존재를 검증한다")
    void saveTaskRejectsUnknownAssignee() {
        when(adminUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> adminOperationTaskService.saveTask(new AdminOperationTaskSaveRequest(
                1L,
                "작업",
                "설명",
                "TODO",
                "LOW",
                99L,
                null,
                "N"
        )));
    }

    @Test
    @DisplayName("운영 작업 상태 변경은 상태만 갱신하고 로그를 남긴다")
    void updateStatusChangesStatus() {
        AdminOperationTask task = AdminOperationTask.builder()
                .taskNo(7L)
                .title("점검")
                .description("설명")
                .status("TODO")
                .priority("HIGH")
                .isPinned("N")
                .build();
        when(adminOperationTaskRepository.findById(7L)).thenReturn(Optional.of(task));

        adminOperationTaskService.updateStatus(7L, "DONE");

        assertEquals("DONE", task.getStatus());
        verify(adminLogService).recordCurrentAdminLog("TASK_STATUS_UPDATE", 7L);
    }

    @Test
    @DisplayName("운영 작업 상세는 단건 응답을 반환한다")
    void getTaskDetailReturnsItem() {
        AdminOperationTask task = AdminOperationTask.builder()
                .taskNo(11L)
                .title("배치 정리")
                .description("배치 스케줄 점검")
                .status("IN_PROGRESS")
                .priority("MEDIUM")
                .assigneeAdminNo(3L)
                .dueDate(LocalDate.of(2026, 5, 22))
                .isPinned("Y")
                .build();
        when(adminOperationTaskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(adminUserRepository.findById(3L)).thenReturn(Optional.of(
                AdminUser.builder().adminNo(3L).name("담당자").loginId("assignee").password("pw").build()
        ));
        when(adminUserRepository.findAll()).thenReturn(List.of(
                AdminUser.builder().adminNo(2L).name("운영자").loginId("ops").password("pw").build(),
                AdminUser.builder().adminNo(3L).name("담당자").loginId("assignee").password("pw").build()
        ));
        when(adminOperationTaskRepository.getTaskAssignmentRecommendations(any(LocalDate.class), eq(3L), eq(3)))
                .thenReturn(List.of(
                        new AdminOperationTaskAssigneeRecommendationDto(2L, "운영자", 0L, 0L, 0L),
                        new AdminOperationTaskAssigneeRecommendationDto(5L, "지원자", 4L, 2L, 1L)
                ));
        when(adminLogService.getLogList(any(AdminLogListRequest.class), eq(PageRequest.of(0, 5))))
                .thenReturn(new AdminLogListResponse(
                        List.of(new AdminLogListResponse.Item(9L, 1L, "운영자", "TASK_UPDATE", 11L, "운영 작업 #11", "/admin/settings/tasks/get?no=11&returnTo=/admin/settings/tasks", "127.0.0.1", "2026-05-23 10:00")),
                        1L, 1, 0, 5, 1L, 1L, "1-1 / 1건 · 1페이지",
                        new AdminLogListResponse.Summary(1, 1, 0, 1, 0, 1),
                        new AdminLogListResponse.AppliedQuery(null, "TASK_", 11L, null, null),
                        new AdminLogListResponse.ResultMeta("검색 결과 1건", "1-1 / 1건 · 1페이지", 2, "1-1 · 작업=TASK_ · 대상=11")
                ));
        AdminOperationTaskCommentResDto comment = new AdminOperationTaskCommentResDto();
        comment.setCommentNo(30L);
        comment.setTaskNo(11L);
        comment.setAdminNo(3L);
        comment.setAdminName("담당자");
        comment.setContent("우선 확인 필요");
        comment.setCrtDtm(LocalDateTime.of(2026, 5, 23, 12, 0));
        when(adminOperationTaskCommentRepository.getTaskComments(11L, 20)).thenReturn(List.of(comment));

        var result = adminOperationTaskService.getTaskDetail(11L);

        assertEquals(11L, result.taskNo());
        assertEquals("배치 정리", result.title());
        assertEquals("IN_PROGRESS", result.status());
        assertEquals("MEDIUM", result.priority());
        assertEquals("담당자", result.assigneeAdminName());
        assertEquals("/admin/settings/tasks/history?taskNo=11", result.historyPath());
        assertEquals(1, result.recentHistories().size());
        assertEquals("/admin/settings/tasks/history?taskNo=11", result.recentHistories().get(0).historyPath());
        assertEquals(1, result.comments().size());
        assertEquals("우선 확인 필요", result.comments().get(0).content());
        assertEquals(2, result.assigneeOptions().size());
        assertEquals("담당자", result.assigneeOptions().get(0).name());
        assertEquals("운영자", result.assigneeOptions().get(1).name());
        assertEquals(2, result.assignmentRecommendations().size());
        assertEquals(2L, result.assignmentRecommendations().get(0).adminNo());
        assertEquals("현재 배정 작업이 없습니다.", result.assignmentRecommendations().get(0).reasonLabel());
        assertEquals("기한 초과 1건 · 진행중 2건 · 전체 4건", result.assignmentRecommendations().get(1).reasonLabel());
        assertEquals(5L, result.assignmentRecommendations().get(1).adminNo());
    }

    @Test
    @DisplayName("운영 작업 일괄 변경은 실제 변경 건만 로그를 남긴다")
    void bulkOperateUpdatesChangedTasksOnly() {
        AdminOperationTask changedTask = AdminOperationTask.builder()
                .taskNo(21L)
                .title("배치 점검")
                .description("설명")
                .status("TODO")
                .priority("HIGH")
                .assigneeAdminNo(null)
                .isPinned("N")
                .build();
        AdminOperationTask unchangedTask = AdminOperationTask.builder()
                .taskNo(22L)
                .title("공지 점검")
                .description("설명")
                .status("DONE")
                .priority("HIGH")
                .assigneeAdminNo(null)
                .isPinned("N")
                .build();
        when(adminOperationTaskRepository.findAllById(List.of(21L, 22L))).thenReturn(List.of(changedTask, unchangedTask));

        var result = adminOperationTaskService.bulkOperate(
                new AdminOperationTaskBulkOperateRequest(List.of(21L, 22L), "DONE", null, null, null, null)
        );

        assertEquals(2, result.requestedCount());
        assertEquals(1, result.updatedCount());
        assertEquals(1, result.unchangedCount());
        assertEquals("DONE", changedTask.getStatus());
        verify(adminLogService).recordCurrentAdminLog("TASK_BULK_UPDATE", 21L);
    }

    @Test
    @DisplayName("운영 작업 일괄 변경은 담당 해제를 반영한다")
    void bulkOperateClearsAssignee() {
        AdminOperationTask assignedTask = AdminOperationTask.builder()
                .taskNo(31L)
                .title("배정 해제 테스트")
                .description("설명")
                .status("IN_PROGRESS")
                .priority("MEDIUM")
                .assigneeAdminNo(7L)
                .isPinned("N")
                .build();
        when(adminOperationTaskRepository.findAllById(List.of(31L))).thenReturn(List.of(assignedTask));

        var result = adminOperationTaskService.bulkOperate(
                new AdminOperationTaskBulkOperateRequest(List.of(31L), null, null, null, "CLEAR", null)
        );

        assertEquals(1, result.updatedCount());
        assertNull(assignedTask.getAssigneeAdminNo());
    }

    @Test
    @DisplayName("운영 작업 삭제는 존재하는 작업만 삭제하고 로그를 남긴다")
    void deleteTaskDeletesExistingEntity() {
        AdminOperationTask task = AdminOperationTask.builder()
                .taskNo(99L)
                .title("삭제 대상")
                .description("설명")
                .status("TODO")
                .priority("LOW")
                .isPinned("N")
                .build();
        when(adminOperationTaskRepository.findById(99L)).thenReturn(Optional.of(task));

        adminOperationTaskService.deleteTask(99L);

        verify(adminOperationTaskRepository).delete(argThat(item -> item.getTaskNo().equals(99L)));
        verify(adminLogService).recordCurrentAdminLog("TASK_DELETE", 99L);
    }

    @Test
    @DisplayName("운영 작업 일괄 삭제는 코멘트를 함께 정리하고 누락 건수를 반환한다")
    void bulkDeleteRemovesCommentsAndReturnsMissingCounts() {
        AdminOperationTask first = AdminOperationTask.builder()
                .taskNo(41L)
                .title("배치 점검")
                .description("설명")
                .status("TODO")
                .priority("LOW")
                .isPinned("N")
                .build();
        AdminOperationTask third = AdminOperationTask.builder()
                .taskNo(43L)
                .title("공지 점검")
                .description("설명")
                .status("DONE")
                .priority("HIGH")
                .isPinned("Y")
                .build();
        when(adminOperationTaskRepository.findAllById(List.of(41L, 42L, 43L))).thenReturn(List.of(first, third));

        AdminOperationTaskService.BulkDeleteResult result = adminOperationTaskService.bulkDelete(
                new AdminOperationTaskBulkDeleteRequest(List.of(41L, 42L, 43L))
        );

        assertEquals(3, result.requestedCount());
        assertEquals(2, result.deletedCount());
        assertEquals(1, result.missingCount());
        verify(adminOperationTaskCommentRepository).deleteByTaskNoIn(List.of(41L, 43L));
        verify(adminOperationTaskRepository).deleteAll(List.of(first, third));
        verify(adminLogService).recordCurrentAdminLog("TASK_BULK_DELETE", 41L);
        verify(adminLogService).recordCurrentAdminLog("TASK_BULK_DELETE", 43L);
    }

    @Test
    @DisplayName("운영 작업 메모 등록은 로그를 남긴다")
    void addCommentSavesCommentAndRecordsLog() {
        AdminOperationTask task = AdminOperationTask.builder()
                .taskNo(31L)
                .title("공지 작업")
                .description("설명")
                .status("TODO")
                .priority("MEDIUM")
                .isPinned("N")
                .build();
        when(adminOperationTaskRepository.findById(31L)).thenReturn(Optional.of(task));

        adminOperationTaskService.addComment(31L, new AdminOperationTaskCommentSaveRequest("  메모 남김  "));

        verify(adminOperationTaskCommentRepository).save(any(AdminOperationTaskComment.class));
        verify(adminLogService).recordCurrentAdminLog("TASK_COMMENT_CREATE", 31L);
    }

    @Test
    @DisplayName("운영 작업 메모 삭제는 같은 작업 메모만 삭제한다")
    void deleteCommentDeletesMatchingComment() {
        AdminOperationTask task = AdminOperationTask.builder()
                .taskNo(32L)
                .title("배치 점검")
                .description("설명")
                .status("TODO")
                .priority("LOW")
                .isPinned("N")
                .build();
        AdminOperationTaskComment comment = AdminOperationTaskComment.builder()
                .commentNo(41L)
                .taskNo(32L)
                .content("삭제 대상")
                .build();
        when(adminOperationTaskRepository.findById(32L)).thenReturn(Optional.of(task));
        when(adminOperationTaskCommentRepository.findById(41L)).thenReturn(Optional.of(comment));

        adminOperationTaskService.deleteComment(32L, 41L);

        verify(adminOperationTaskCommentRepository).delete(comment);
        verify(adminLogService).recordCurrentAdminLog("TASK_COMMENT_DELETE", 32L);
    }

    @Test
    @DisplayName("운영 작업 메모 수정은 같은 작업 메모만 갱신하고 로그를 남긴다")
    void updateCommentUpdatesMatchingComment() {
        AdminOperationTask task = AdminOperationTask.builder()
                .taskNo(33L)
                .title("배치 점검")
                .description("설명")
                .status("TODO")
                .priority("LOW")
                .isPinned("N")
                .build();
        AdminOperationTaskComment comment = AdminOperationTaskComment.builder()
                .commentNo(51L)
                .taskNo(33L)
                .content("수정 전")
                .build();
        when(adminOperationTaskRepository.findById(33L)).thenReturn(Optional.of(task));
        when(adminOperationTaskCommentRepository.findById(51L)).thenReturn(Optional.of(comment));

        adminOperationTaskService.updateComment(33L, 51L, new AdminOperationTaskCommentSaveRequest("  수정 후 메모  "));

        assertEquals("수정 후 메모", comment.getContent());
        verify(adminLogService).recordCurrentAdminLog("TASK_COMMENT_UPDATE", 33L);
    }
}
