package com.section.common.system.dto;

public record AdminOperationTaskListQuery(
        String keyword,
        String status,
        String priority,
        Long assigneeAdminNo,
        String isPinned,
        String overdueOnly,
        String unassignedOnly
) {
    public AdminOperationTaskListQuery toStatsQuery() {
        return new AdminOperationTaskListQuery(keyword, null, null, assigneeAdminNo, isPinned, null, unassignedOnly);
    }
}
