package com.section.common.system.dto;

public record AdminOperationTaskSummaryDto(
        long totalCount,
        long todoCount,
        long inProgressCount,
        long overdueCount
) {
}
