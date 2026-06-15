package com.section.admin.task.service;

import com.section.admin.log.req.AdminLogListRequest;
import com.section.admin.log.res.AdminLogListResponse;
import com.section.admin.log.service.AdminLogService;
import com.section.admin.task.req.AdminOperationTaskHistoryListRequest;
import com.section.admin.task.res.AdminOperationTaskHistoryListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOperationTaskHistoryServiceTest {

    @Mock
    private AdminLogService adminLogService;

    @InjectMocks
    private AdminOperationTaskHistoryService adminOperationTaskHistoryService;

    @Test
    @DisplayName("운영 작업 이력 서비스는 작업 로그만 조회하도록 기본 actionType을 설정한다")
    void getTaskHistoryListUsesTaskPrefixQuery() {
        when(adminLogService.getLogList(any(AdminLogListRequest.class), any(Pageable.class)))
                .thenReturn(new AdminLogListResponse(
                        List.of(new AdminLogListResponse.Item(11L, 2L, "운영자", "TASK_STATUS_UPDATE", 7L, "운영 작업 #7", "/admin/settings/tasks/get?no=7&returnTo=/admin/settings/tasks", "127.0.0.1", "2026-05-23 10:00")),
                        1L,
                        1,
                        0,
                        20,
                        1L,
                        1L,
                        "1-1 / 1건 · 1페이지",
                        new AdminLogListResponse.Summary(1, 1, 0, 1, 0, 1),
                        new AdminLogListResponse.AppliedQuery(2L, "운영", "TASK_", 7L, "2026-05-23", "2026-05-23"),
                        new AdminLogListResponse.ResultMeta("검색 결과 1건", "1-1 / 1건 · 1페이지", 3, "1-1 · 작업=TASK_")
                ));

        AdminOperationTaskHistoryListRequest request = new AdminOperationTaskHistoryListRequest();
        request.setTaskNo(7L);
        request.setAdminNo(2L);
        request.setAdminKeyword("운영");
        request.setReturnTo("/admin/settings/tasks?page=1");

        AdminOperationTaskHistoryListResponse response = adminOperationTaskHistoryService.getTaskHistoryList(request, 0, 20);

        ArgumentCaptor<AdminLogListRequest> requestCaptor = ArgumentCaptor.forClass(AdminLogListRequest.class);
        verify(adminLogService).getLogList(requestCaptor.capture(), any(Pageable.class));
        assertEquals("TASK_", requestCaptor.getValue().getActionType());
        assertEquals(7L, requestCaptor.getValue().getTargetId());
        assertEquals("운영", requestCaptor.getValue().getAdminKeyword());
        assertEquals(1, response.items().size());
        assertEquals("상태 변경", response.items().get(0).actionLabel());
        assertEquals("/admin/settings/tasks?page=1", response.appliedQuery().returnTo());
    }

    @Test
    @DisplayName("운영 작업 이력 CSV 내보내기는 작업 로그 필터를 그대로 전달한다")
    void exportTaskHistoryCsvDelegatesToLogExport() {
        when(adminLogService.exportLogListCsv(any(AdminLogListRequest.class)))
                .thenReturn("csv".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        AdminOperationTaskHistoryListRequest request = new AdminOperationTaskHistoryListRequest();
        request.setTaskNo(9L);
        request.setActionType(" task_comment_update ");

        byte[] response = adminOperationTaskHistoryService.exportTaskHistoryCsv(request);

        ArgumentCaptor<AdminLogListRequest> requestCaptor = ArgumentCaptor.forClass(AdminLogListRequest.class);
        verify(adminLogService).exportLogListCsv(requestCaptor.capture());
        assertEquals(9L, requestCaptor.getValue().getTargetId());
        assertEquals("TASK_COMMENT_UPDATE", requestCaptor.getValue().getActionType());
        assertEquals("csv", new String(response, java.nio.charset.StandardCharsets.UTF_8));
    }
}
