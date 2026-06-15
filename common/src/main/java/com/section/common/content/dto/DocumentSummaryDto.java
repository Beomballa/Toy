package com.section.common.content.dto;

public record DocumentSummaryDto(
        long totalCount,
        long publishedCount,
        long draftCount,
        long publicCount,
        long privateCount,
        long pinnedCount,
        long linkedCount,
        long totalViewCount
) {
}
