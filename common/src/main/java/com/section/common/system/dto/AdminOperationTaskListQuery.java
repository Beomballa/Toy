package com.section.common.system.dto;

public record AdminOperationTaskListQuery(
        String keyword,
        String status,
        String priority,
        Long assigneeAdminNo,
        String overdueOnly
) {
    public AdminOperationTaskListQuery toStatsQuery() {
        return new AdminOperationTaskListQuery(keyword, null, null, assigneeAdminNo, null);
    }
}
