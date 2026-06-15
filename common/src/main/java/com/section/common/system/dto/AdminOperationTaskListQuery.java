package com.section.common.system.dto;

import java.time.LocalDate;

public record AdminOperationTaskListQuery(
        String keyword,
        Long taskNo,
        String status,
        String priority,
        Long assigneeAdminNo,
        String isPinned,
        String overdueOnly,
        String unassignedOnly,
        String commentedOnly,
        Integer dueWithinDays,
        String dueState,
        String sortBy,
        LocalDate dueDateFrom,
        LocalDate dueDateTo
) {
    public AdminOperationTaskListQuery toStatsQuery() {
        return new AdminOperationTaskListQuery(
                keyword,
                taskNo,
                null,
                null,
                assigneeAdminNo,
                isPinned,
                null,
                unassignedOnly,
                commentedOnly,
                dueWithinDays,
                dueState,
                sortBy,
                dueDateFrom,
                dueDateTo
        );
    }
}
