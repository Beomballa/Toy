package com.section.common.system.dto;

public record AdminOperationTaskWorkloadSummaryDto(
        long assigneeCount,
        long assignedTaskCount,
        long overdueTaskCount,
        long unassignedTaskCount
) {
}
