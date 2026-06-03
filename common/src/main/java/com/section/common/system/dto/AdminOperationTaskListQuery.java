package com.section.common.system.dto;

import java.time.LocalDate;

public record AdminOperationTaskListQuery(
        String keyword,
        String status,
        String priority,
        Long assigneeAdminNo,
        String isPinned,
        String overdueOnly,
        String unassignedOnly,
        String commentedOnly,
        String sortBy,
        LocalDate dueDateFrom,
        LocalDate dueDateTo
) {
    public AdminOperationTaskListQuery toStatsQuery() {
        return new AdminOperationTaskListQuery(
                keyword,
                null,
                null,
                assigneeAdminNo,
                isPinned,
                null,
                unassignedOnly,
                commentedOnly,
                sortBy,
                dueDateFrom,
                dueDateTo
        );
    }
}
