package com.section.admin.notice.service;

import com.section.admin.log.req.AdminLogListRequest;
import com.section.admin.log.res.AdminLogListResponse;
import com.section.admin.log.service.AdminLogService;
import com.section.admin.notice.req.AdminOperationNoticeHistoryListRequest;
import com.section.admin.notice.res.AdminOperationNoticeHistoryListResponse;
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
class AdminOperationNoticeHistoryServiceTest {

    @Mock
    private AdminLogService adminLogService;

    @InjectMocks
    private AdminOperationNoticeHistoryService adminOperationNoticeHistoryService;

    @Test
    @DisplayName("운영 공지 이력 서비스는 공지 로그만 조회하도록 기본 actionType을 설정한다")
    void getNoticeHistoryListUsesNoticePrefixQuery() {
        when(adminLogService.getLogList(any(AdminLogListRequest.class), any(Pageable.class)))
                .thenReturn(new AdminLogListResponse(
                        List.of(new AdminLogListResponse.Item(11L, 2L, "운영자", "NOTICE_UPDATE", 7L, "운영 공지 #7", "/admin/settings/notices?noticeNo=7", "127.0.0.1", "2026-05-20 10:00")),
                        1L,
                        1,
                        0,
                        20,
                        1L,
                        1L,
                        "1-1 / 1건 · 1페이지",
                        new AdminLogListResponse.Summary(1, 1, 1, 0, 0, 1),
                        new AdminLogListResponse.AppliedQuery(2L, null, "NOTICE_", 7L, "2026-05-19", "2026-05-20"),
                        new AdminLogListResponse.ResultMeta("검색 결과 1건", "1-1 / 1건 · 1페이지", 3, "1-1 · 작업=NOTICE_")
                ));

        AdminOperationNoticeHistoryListRequest request = new AdminOperationNoticeHistoryListRequest();
        request.setNoticeNo(7L);
        request.setAdminNo(2L);
        request.setReturnTo("/admin/settings/notices?page=1");

        AdminOperationNoticeHistoryListResponse response = adminOperationNoticeHistoryService.getNoticeHistoryList(request, 0, 20);

        ArgumentCaptor<AdminLogListRequest> requestCaptor = ArgumentCaptor.forClass(AdminLogListRequest.class);
        verify(adminLogService).getLogList(requestCaptor.capture(), any(Pageable.class));
        assertEquals("NOTICE_", requestCaptor.getValue().getActionType());
        assertEquals(7L, requestCaptor.getValue().getTargetId());
        assertEquals(1, response.items().size());
        assertEquals("공지 수정", response.items().get(0).actionLabel());
        assertEquals("/admin/settings/notices?page=1", response.appliedQuery().returnTo());
    }
}
