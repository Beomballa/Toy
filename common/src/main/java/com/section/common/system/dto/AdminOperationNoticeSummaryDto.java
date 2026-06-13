package com.section.common.system.dto;

public record AdminOperationNoticeSummaryDto(
        long totalCount,
        long liveCount,
        long scheduledCount,
        long endedCount,
        long inactiveCount,
        long pinnedCount
) {
}
