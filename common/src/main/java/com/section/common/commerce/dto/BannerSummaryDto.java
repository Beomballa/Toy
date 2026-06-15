package com.section.common.commerce.dto;

public record BannerSummaryDto(
        long totalCount,
        long liveCount,
        long scheduledCount,
        long endedCount,
        long inactiveCount
) {
}
