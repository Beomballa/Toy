package com.section.common.system.dto;

public record AdminUserSummaryDto(
        long totalCount,
        long activeCount,
        long suspendedCount,
        long superCount,
        long inactiveCount
) {
}
