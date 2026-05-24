package com.section.common.system.dto;

public record AdminOperationTaskWorkloadDto(
        Long assigneeAdminNo,
        String assigneeAdminName,
        long totalCount,
        long todoCount,
        long inProgressCount,
        long overdueCount
) {
}
