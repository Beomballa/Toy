package com.section.admin.notice.service;

import com.section.admin.log.res.AdminLogListResponse;
import com.section.admin.log.service.AdminLogService;
import com.section.admin.notice.req.AdminOperationNoticeHistoryListRequest;
import com.section.admin.notice.res.AdminOperationNoticeHistoryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOperationNoticeHistoryService {

    private final AdminLogService adminLogService;

    public AdminOperationNoticeHistoryListResponse getNoticeHistoryList(
            AdminOperationNoticeHistoryListRequest req,
            Integer page,
            Integer size
    ) {
        AdminLogListResponse response = adminLogService.getLogList(
                req.toLogListRequest(),
                PageRequest.of(req.normalizedPage(page), req.normalizedSize(size))
        );
        return AdminOperationNoticeHistoryListResponse.from(response, req.normalizedReturnTo());
    }
}
