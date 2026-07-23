package com.section.common.content.dto;

public record ContentViewSummaryRow(
        long totalViews,
        long uniqueVisitors,
        long viewedContentCount
) {
}
