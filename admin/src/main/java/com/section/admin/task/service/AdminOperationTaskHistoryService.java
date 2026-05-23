package com.section.admin.task.service;

import com.section.admin.log.res.AdminLogListResponse;
import com.section.admin.log.service.AdminLogService;
import com.section.admin.task.req.AdminOperationTaskHistoryListRequest;
import com.section.admin.task.res.AdminOperationTaskHistoryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOperationTaskHistoryService {

    private final AdminLogService adminLogService;

    public AdminOperationTaskHistoryListResponse getTaskHistoryList(
            AdminOperationTaskHistoryListRequest req,
            Integer page,
            Integer size
    ) {
        AdminLogListResponse response = adminLogService.getLogList(
                req.toLogListRequest(),
                PageRequest.of(req.normalizedPage(page), req.normalizedSize(size))
        );
        return AdminOperationTaskHistoryListResponse.from(response, req.normalizedReturnTo());
    }
}
