package com.section.common.commerce.dto;

public record BrandSummaryDto(
        long totalCount,
        long activeCount,
        long inactiveCount
) {
}
