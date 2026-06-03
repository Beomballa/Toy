package com.section.admin.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.section.admin.task.req.AdminOperationTaskBulkDeleteRequest;
import com.section.admin.task.req.AdminOperationTaskBulkOperateRequest;
import com.section.admin.task.req.AdminOperationTaskCommentSaveRequest;
import com.section.admin.task.req.AdminOperationTaskSaveRequest;
import com.section.admin.task.req.AdminOperationTaskWorkloadListRequest;
import com.section.admin.task.res.AdminOperationTaskDetailResponse;
import com.section.admin.task.res.AdminOperationTaskHistoryListResponse;
import com.section.admin.task.res.AdminOperationTaskListResponse;
import com.section.admin.task.res.AdminOperationTaskWorkloadDetailResponse;
import com.section.admin.task.res.AdminOperationTaskWorkloadListResponse;
import com.section.admin.task.service.AdminOperationTaskHistoryService;
import com.section.admin.task.service.AdminOperationTaskService;
import com.section.admin.task.service.AdminOperationTaskWorkloadService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    private AdminOperationTaskHistoryService adminOperationTaskHistoryService;
    @Mock
    private AdminOperationTaskWorkloadService adminOperationTaskWorkloadService;
    @Mock
    private AdminOperationPolicyService adminOperationPolicyService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOperationTaskRestController(adminOperationTaskService, adminOperationTaskHistoryService, adminOperationTaskWorkloadService, adminOperationPolicyService))
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("운영 작업 목록 API는 운영형 페이지 응답을 반환한다")
    void getListReturnsPagedResponse() throws Exception {
        when(adminOperationTaskService.getTaskList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminOperationTaskListResponse(
                        List.of(new AdminOperationTaskListResponse.Item(1L, "정산 확인", "정산 마감", "TODO", "대기", "HIGH", "높음", 2L, "운영자", "2026-05-22", "오늘 마감", "Y", 2L, "최근 메모", "운영자 · 2026-05-21 09:00", "2026-05-21 10:00", "/admin/settings/tasks/history?taskNo=1", "이력", "/admin/settings/logs?actionType=TASK_&targetId=1", "활동 로그")),
                        0,
                        1,
                        1L,
                        10,
                        new AdminOperationTaskListResponse.TaskStats(4L, 2L, 1L, 1L, 1L, "기본 문맥 기준", "고정 우선 · 마감 임박 순"),
                        List.of(new AdminOperationTaskListResponse.AssigneeOption(2L, "운영자")),
                        new AdminOperationTaskListResponse.AppliedQuery(null, null, null, null, null, null, null, null, "PINNED_DUE", null, null),
                        new AdminOperationTaskListResponse.ResultMeta("전체 1건", "1-1 / 1건 · 1페이지", 0, false, "고정 우선 · 마감 임박 순 · 정렬=고정 우선 · 마감 임박 순", 1L, 1L)
                ));

        mockMvc.perform(get("/api/admin/settings/tasks/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("정산 확인"))
                .andExpect(jsonPath("$.items[0].historyPath").value("/admin/settings/tasks/history?taskNo=1"))
                .andExpect(jsonPath("$.items[0].commentCount").value(2))
                .andExpect(jsonPath("$.taskStats.todoCount").value(2L))
                .andExpect(jsonPath("$.assigneeOptions[0].name").value("운영자"))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("전체 1건"))
                .andExpect(jsonPath("$.appliedQuery.sortBy").value("PINNED_DUE"));
    }

    @Test
    @DisplayName("운영 작업 목록 API는 기한 범위 필터를 응답에 포함한다")
    void getListIncludesDueDateRangeFilter() throws Exception {
        when(adminOperationTaskService.getTaskList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminOperationTaskListResponse(
                        List.of(),
                        0,
                        0,
                        0L,
                        10,
                        new AdminOperationTaskListResponse.TaskStats(0L, 0L, 0L, 0L, 0L, "탐색 문맥 기준", "고정 우선 · 마감 임박 순 · 기한=2026-06-01~2026-06-30"),
                        List.of(),
                        new AdminOperationTaskListResponse.AppliedQuery(null, null, null, null, null, null, null, "Y", "PRIORITY_DESC", "2026-06-01", "2026-06-30"),
                        new AdminOperationTaskListResponse.ResultMeta("검색 결과 0건", "조건에 맞는 운영 작업이 없습니다.", 4L, true, "고정 우선 · 마감 임박 순 · 메모있는 작업만 · 기한=2026-06-01~2026-06-30 · 정렬=우선순위 높은 순", 0L, 0L)
                ));

        mockMvc.perform(get("/api/admin/settings/tasks/list")
                        .param("commentedOnly", "Y")
                        .param("sortBy", "PRIORITY_DESC")
                        .param("dueDateFrom", "2026-06-01")
                        .param("dueDateTo", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedQuery.commentedOnly").value("Y"))
                .andExpect(jsonPath("$.appliedQuery.sortBy").value("PRIORITY_DESC"))
                .andExpect(jsonPath("$.appliedQuery.dueDateFrom").value("2026-06-01"))
                .andExpect(jsonPath("$.appliedQuery.dueDateTo").value("2026-06-30"))
                .andExpect(jsonPath("$.resultMeta.appliedFilterCount").value(4));
    }

    @Test
    @DisplayName("운영 작업 CSV 내보내기는 다운로드 헤더를 반환한다")
    void exportReturnsAttachmentHeaders() throws Exception {
        when(adminOperationTaskService.exportTaskListCsv(org.mockito.ArgumentMatchers.any()))
                .thenReturn("csv".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/admin/settings/tasks/export").param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=\"tasks-")));
    }

    @Test
    @DisplayName("운영 작업 워크로드 API는 페이지 응답을 반환한다")
    void getWorkloadsReturnsPagedResponse() throws Exception {
        when(adminOperationTaskWorkloadService.getWorkloadList(org.mockito.ArgumentMatchers.any(AdminOperationTaskWorkloadListRequest.class)))
                .thenReturn(new AdminOperationTaskWorkloadListResponse(
                        List.of(new AdminOperationTaskWorkloadListResponse.Item(2L, "운영자", 6L, 2L, 3L, 1L, "정산 점검", "우선 확인 필요", "관리자", "2026-05-25 10:00", "/admin/settings/tasks?assigneeAdminNo=2", "/admin/settings/tasks?assigneeAdminNo=2&overdueOnly=Y")),
                        0,
                        1,
                        1L,
                        10,
                        new AdminOperationTaskWorkloadListResponse.Summary(1L, 6L, 1L, 2L, "탐색 문맥 기준", "기한 초과 우선 · 진행중 우선"),
                        new AdminOperationTaskWorkloadListResponse.AppliedQuery("정산", "HIGH", null),
                        new AdminOperationTaskWorkloadListResponse.ResultMeta("검색 결과 1명", "1-1 / 1명 · 1페이지", 2L, true, "기한 초과 우선 · 진행중 우선", 1L, 1L)
                ));

        mockMvc.perform(get("/api/admin/settings/tasks/workloads/list").param("keyword", "정산"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].assigneeAdminName").value("운영자"))
                .andExpect(jsonPath("$.items[0].latestCommentContent").value("우선 확인 필요"))
                .andExpect(jsonPath("$.summary.assignedTaskCount").value(6L))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("검색 결과 1명"));
    }

    @Test
    @DisplayName("운영 작업 워크로드 상세 API는 단건 응답을 반환한다")
    void getWorkloadDetailReturnsItem() throws Exception {
        when(adminOperationTaskWorkloadService.getWorkloadDetail(7L))
                .thenReturn(new AdminOperationTaskWorkloadDetailResponse(
                        7L,
                        "운영자",
                        new AdminOperationTaskWorkloadDetailResponse.Summary(6L, 2L, 3L, 1L),
                        "/admin/settings/tasks?assigneeAdminNo=7",
                        "/admin/settings/tasks?assigneeAdminNo=7&status=TODO",
                        "/admin/settings/tasks?assigneeAdminNo=7&status=IN_PROGRESS",
                        "/admin/settings/tasks?assigneeAdminNo=7&overdueOnly=Y",
                        "/admin/settings/logs?adminNo=7&actionType=TASK_",
                        List.of(new AdminOperationTaskWorkloadDetailResponse.RecentTask(11L, "정산 점검", "진행중", "높음", "2026-05-26", "/admin/settings/tasks/get?no=11", "/admin/settings/tasks/history?taskNo=11")),
                        List.of(new AdminOperationTaskWorkloadDetailResponse.RecentTask(12L, "배송 지연 확인", "대기", "중간", "기한 초과", "/admin/settings/tasks/get?no=12", "/admin/settings/tasks/history?taskNo=12")),
                        List.of(new AdminOperationTaskWorkloadDetailResponse.RecentComment(31L, 11L, "정산 점검", "관리자", "우선 확인 필요", "2026-05-25 11:00", "/admin/settings/tasks/get?no=11")),
                        List.of(new AdminOperationTaskWorkloadDetailResponse.RecentHistory(15L, 11L, "운영 작업 #11", "작업 수정", "운영자", "2026-05-25 12:00", "/admin/settings/tasks/get?no=11", "/api/admin/logs/get?no=15"))
                ));

        mockMvc.perform(get("/api/admin/settings/tasks/workloads/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeAdminName").value("운영자"))
                .andExpect(jsonPath("$.summary.totalCount").value(6L))
                .andExpect(jsonPath("$.todoPath").value("/admin/settings/tasks?assigneeAdminNo=7&status=TODO"))
                .andExpect(jsonPath("$.inProgressPath").value("/admin/settings/tasks?assigneeAdminNo=7&status=IN_PROGRESS"))
                .andExpect(jsonPath("$.overdueTasks[0].title").value("배송 지연 확인"))
                .andExpect(jsonPath("$.recentComments[0].content").value("우선 확인 필요"))
                .andExpect(jsonPath("$.recentHistories[0].actionLabel").value("작업 수정"));
    }

    @Test
    @DisplayName("운영 작업 상세 API는 단건 응답을 반환한다")
    void getDetailReturnsItem() throws Exception {
        when(adminOperationTaskService.getTaskDetail(3L))
                .thenReturn(new AdminOperationTaskDetailResponse(
                        3L, "정산 확인", "정산 마감", "TODO", "대기", "HIGH", "높음", 2L, "운영자", "2026-05-22", "오늘 마감", "Y", "2026-05-23 10:00",
                        "/admin/settings/tasks/history?taskNo=3",
                        "/admin/settings/logs?actionType=TASK_&targetId=3",
                        List.of(new AdminOperationTaskDetailResponse.AssigneeOption(2L, "운영자")),
                        List.of(new AdminOperationTaskDetailResponse.AssignmentRecommendation(5L, "지원자", 1L, 0L, 0L, "기한 초과 없이 운영 중입니다.")),
                        List.of(new AdminOperationTaskDetailResponse.RecentHistory(8L, "TASK_UPDATE", "작업 수정", "운영자", "2026-05-23 11:00", "/admin/settings/logs?actionType=TASK_UPDATE&targetId=3", "/admin/settings/tasks/history?taskNo=3")),
                        List.of(new AdminOperationTaskDetailResponse.Comment(14L, 2L, "운영자", "메모", "2026-05-23 11:05"))
                ));

        mockMvc.perform(get("/api/admin/settings/tasks/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskNo").value(3L))
                .andExpect(jsonPath("$.title").value("정산 확인"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.historyPath").value("/admin/settings/tasks/history?taskNo=3"))
                .andExpect(jsonPath("$.activityLogPath").value("/admin/settings/logs?actionType=TASK_&targetId=3"))
                .andExpect(jsonPath("$.assigneeOptions[0].name").value("운영자"))
                .andExpect(jsonPath("$.assignmentRecommendations[0].adminName").value("지원자"))
                .andExpect(jsonPath("$.recentHistories[0].actionLabel").value("작업 수정"))
                .andExpect(jsonPath("$.comments[0].content").value("메모"));
    }

    @Test
    @DisplayName("운영 작업 이력 API는 전용 페이지 응답을 반환한다")
    void getHistoryListReturnsPagedResponse() throws Exception {
        when(adminOperationTaskHistoryService.getTaskHistoryList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminOperationTaskHistoryListResponse(
                        List.of(new AdminOperationTaskHistoryListResponse.Item(5L, 3L, "운영 작업 #3", "/admin/settings/tasks/get?no=3&returnTo=%2Fadmin%2Fsettings%2Ftasks", "TASK_STATUS_UPDATE", "상태 변경", 2L, "운영자", "127.0.0.1", "2026-05-23 12:00", "/api/admin/logs/get?no=5")),
                        1L,
                        1,
                        0,
                        20,
                        1L,
                        1L,
                        "1-1 / 1건 · 1페이지",
                        new AdminOperationTaskHistoryListResponse.AppliedQuery(3L, "TASK_STATUS_UPDATE", 2L, "2026-05-23", "2026-05-23", "/admin/settings/tasks"),
                        new AdminOperationTaskHistoryListResponse.ResultMeta("검색 결과 1건", "1-1 / 1건 · 1페이지", 4, "1-1 · 작업=TASK_STATUS_UPDATE")
                ));

        mockMvc.perform(get("/api/admin/settings/tasks/history/list").param("taskNo", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].taskNo").value(3L))
                .andExpect(jsonPath("$.items[0].actionLabel").value("상태 변경"))
                .andExpect(jsonPath("$.resultMeta.filterCount").value(4));
    }

    @Test
    @DisplayName("운영 작업 이력 CSV 내보내기는 다운로드 헤더를 반환한다")
    void exportHistoryReturnsAttachmentHeaders() throws Exception {
        when(adminOperationTaskHistoryService.exportTaskHistoryCsv(org.mockito.ArgumentMatchers.any()))
                .thenReturn("csv".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/admin/settings/tasks/history/export").param("taskNo", "3"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=\"task-history-")));
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

    @Test
    @DisplayName("운영 작업 일괄 변경 API는 결과 응답을 반환한다")
    void bulkOperateReturnsResult() throws Exception {
        when(adminOperationTaskService.bulkOperate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminOperationTaskService.BulkOperateResult(3, 2, 1));

        mockMvc.perform(post("/api/admin/settings/tasks/bulk-operate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AdminOperationTaskBulkOperateRequest(List.of(1L, 2L, 3L), "DONE", null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(3))
                .andExpect(jsonPath("$.updatedCount").value(2))
                .andExpect(jsonPath("$.unchangedCount").value(1));
    }

    @Test
    @DisplayName("운영 작업 일괄 삭제 API는 삭제 결과 응답을 반환한다")
    void bulkDeleteReturnsResult() throws Exception {
        when(adminOperationTaskService.bulkDelete(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminOperationTaskService.BulkDeleteResult(3, 2, 1));

        mockMvc.perform(post("/api/admin/settings/tasks/bulk-delete")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AdminOperationTaskBulkDeleteRequest(List.of(1L, 2L, 3L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(3))
                .andExpect(jsonPath("$.deletedCount").value(2))
                .andExpect(jsonPath("$.missingCount").value(1));
    }

    @Test
    @DisplayName("운영 작업 메모 등록 API는 성공 응답을 반환한다")
    void addCommentReturnsOk() throws Exception {
        mockMvc.perform(post("/api/admin/settings/tasks/3/comments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AdminOperationTaskCommentSaveRequest("메모"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"));
    }

    @Test
    @DisplayName("운영 작업 메모 삭제 API는 성공 응답을 반환한다")
    void deleteCommentReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/settings/tasks/3/comments/11"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("운영 작업 메모 수정 API는 성공 응답을 반환한다")
    void updateCommentReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/admin/settings/tasks/3/comments/11")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AdminOperationTaskCommentSaveRequest("수정 메모"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"));
    }
}
