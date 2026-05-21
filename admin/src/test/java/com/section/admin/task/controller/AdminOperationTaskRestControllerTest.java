package com.section.admin.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.section.admin.task.req.AdminOperationTaskSaveRequest;
import com.section.admin.task.res.AdminOperationTaskDetailResponse;
import com.section.admin.task.res.AdminOperationTaskListResponse;
import com.section.admin.task.service.AdminOperationTaskService;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOperationTaskRestControllerTest {

    @Mock
    private AdminOperationTaskService adminOperationTaskService;
    @Mock
    private AdminOperationPolicyService adminOperationPolicyService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOperationTaskRestController(adminOperationTaskService, adminOperationPolicyService))
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("운영 작업 목록 API는 운영형 페이지 응답을 반환한다")
    void getListReturnsPagedResponse() throws Exception {
        when(adminOperationTaskService.getTaskList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminOperationTaskListResponse(
                        List.of(new AdminOperationTaskListResponse.Item(1L, "정산 확인", "정산 마감", "TODO", "대기", "HIGH", "높음", 2L, "운영자", "2026-05-22", "오늘 마감", "Y", "2026-05-21 10:00", "/admin/settings/logs?actionType=TASK_&targetId=1", "활동 로그")),
                        0,
                        1,
                        1L,
                        10,
                        new AdminOperationTaskListResponse.TaskStats(4L, 2L, 1L, 1L, "기본 문맥 기준", "고정 우선 · 마감 임박 순"),
                        List.of(new AdminOperationTaskListResponse.AssigneeOption(2L, "운영자")),
                        new AdminOperationTaskListResponse.AppliedQuery(null, null, null, null, null),
                        new AdminOperationTaskListResponse.ResultMeta("전체 1건", "1-1 / 1건 · 1페이지", 0, false, "고정 우선 · 마감 임박 순", 1L, 1L)
                ));

        mockMvc.perform(get("/api/admin/settings/tasks/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("정산 확인"))
                .andExpect(jsonPath("$.taskStats.todoCount").value(2L))
                .andExpect(jsonPath("$.assigneeOptions[0].name").value("운영자"))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("전체 1건"));
    }

    @Test
    @DisplayName("운영 작업 상세 API는 단건 응답을 반환한다")
    void getDetailReturnsItem() throws Exception {
        when(adminOperationTaskService.getTaskDetail(3L))
                .thenReturn(new AdminOperationTaskDetailResponse(
                        3L, "정산 확인", "정산 마감", "TODO", "HIGH", 2L, "2026-05-22", "Y"
                ));

        mockMvc.perform(get("/api/admin/settings/tasks/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskNo").value(3L))
                .andExpect(jsonPath("$.title").value("정산 확인"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    @DisplayName("유지보수 모드에서는 운영 작업 저장이 차단된다")
    void saveReturnsServiceUnavailableWhenMaintenanceModeEnabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_MAINTENANCE_MODE))
                .when(adminOperationPolicyService)
                .assertAdminWriteAllowed();

        mockMvc.perform(post("/api/admin/settings/tasks/save")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AdminOperationTaskSaveRequest(
                                null, "정산 확인", "설명", "TODO", "HIGH", null, null, "N"
                        ))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    @DisplayName("운영 작업 상태 변경 API는 성공 응답을 반환한다")
    void updateStatusReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/admin/settings/tasks/status/3?status=DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"));
    }

    @Test
    @DisplayName("운영 작업 삭제 API는 성공 응답을 반환한다")
    void deleteReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/settings/tasks/delete").param("no", "3"))
                .andExpect(status().isOk());
    }
}
