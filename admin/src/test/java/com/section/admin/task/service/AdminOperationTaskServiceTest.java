package com.section.admin.task.service;

import com.section.admin.log.req.AdminLogListRequest;
import com.section.admin.log.res.AdminLogListResponse;
import com.section.admin.log.service.AdminLogService;
import com.section.admin.task.req.AdminOperationTaskListRequest;
import com.section.admin.task.req.AdminOperationTaskSaveRequest;
import com.section.admin.task.res.AdminOperationTaskListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.system.dto.AdminOperationTaskListQuery;
import com.section.common.system.dto.AdminOperationTaskListResDto;
import com.section.common.system.dto.AdminOperationTaskSummaryDto;
import com.section.common.system.entity.AdminOperationTask;
import com.section.common.system.entity.AdminUser;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOperationTaskServiceTest {

    @Mock
    private AdminOperationTaskRepository adminOperationTaskRepository;
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
                .thenReturn(new AdminOperationTaskSummaryDto(5, 2, 2, 1));
        when(adminUserRepository.findAll()).thenReturn(List.of(
                AdminUser.builder().adminNo(2L).name("운영자").loginId("ops").password("pw").build()
        ));

        AdminOperationTaskListResponse response = adminOperationTaskService.getTaskList(request);

        assertEquals(1, response.items().size());
        assertEquals(1L, response.totalElements());
        assertEquals(5L, response.taskStats().totalCount());
        assertEquals("운영자", response.assigneeOptions().get(0).name());
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
        when(adminLogService.getLogList(any(AdminLogListRequest.class), eq(PageRequest.of(0, 5))))
                .thenReturn(new AdminLogListResponse(
                        List.of(new AdminLogListResponse.Item(9L, 1L, "운영자", "TASK_UPDATE", 11L, "운영 작업 #11", "/admin/settings/tasks/get?no=11&returnTo=/admin/settings/tasks", "127.0.0.1", "2026-05-23 10:00")),
                        1L, 1, 0, 5, 1L, 1L, "1-1 / 1건 · 1페이지",
                        new AdminLogListResponse.AppliedQuery(null, "TASK_", 11L, null, null),
                        new AdminLogListResponse.ResultMeta("검색 결과 1건", "1-1 / 1건 · 1페이지", 2, "1-1 · 작업=TASK_ · 대상=11")
                ));

        var result = adminOperationTaskService.getTaskDetail(11L);

        assertEquals(11L, result.taskNo());
        assertEquals("배치 정리", result.title());
        assertEquals("IN_PROGRESS", result.status());
        assertEquals("MEDIUM", result.priority());
        assertEquals("담당자", result.assigneeAdminName());
        assertEquals(1, result.recentHistories().size());
    }
}
