package com.section.admin.task.service;

import com.section.admin.task.req.AdminOperationTaskWorkloadListRequest;
import com.section.admin.task.res.AdminOperationTaskWorkloadListResponse;
import com.section.common.system.dto.AdminOperationTaskWorkloadCommentSummaryDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadListQuery;
import com.section.common.system.dto.AdminOperationTaskWorkloadSummaryDto;
import com.section.common.system.repository.AdminOperationTaskCommentRepository;
import com.section.common.system.repository.AdminOperationTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOperationTaskWorkloadService {

    private final AdminOperationTaskRepository adminOperationTaskRepository;
    private final AdminOperationTaskCommentRepository adminOperationTaskCommentRepository;

    public AdminOperationTaskWorkloadListResponse getWorkloadList(AdminOperationTaskWorkloadListRequest req) {
        AdminOperationTaskWorkloadListQuery query = req.toQuery();
        LocalDate today = LocalDate.now();
        Page<AdminOperationTaskWorkloadDto> page = adminOperationTaskRepository.getTaskWorkloadPage(
                query,
                PageRequest.of(req.normalizedPage(), req.normalizedSize()),
                today
        );
        AdminOperationTaskWorkloadSummaryDto summary = adminOperationTaskRepository.getTaskWorkloadSummary(query, today);
        Map<Long, AdminOperationTaskWorkloadCommentSummaryDto> latestCommentMap = adminOperationTaskCommentRepository
                .getLatestCommentsByAssigneeAdminNos(
                        page.getContent().stream()
                                .map(AdminOperationTaskWorkloadDto::assigneeAdminNo)
                                .filter(Objects::nonNull)
                                .toList()
                ).stream()
                .collect(Collectors.toMap(AdminOperationTaskWorkloadCommentSummaryDto::getAssigneeAdminNo, item -> item));
        return AdminOperationTaskWorkloadListResponse.of(page, query, summary, latestCommentMap);
    }
}
