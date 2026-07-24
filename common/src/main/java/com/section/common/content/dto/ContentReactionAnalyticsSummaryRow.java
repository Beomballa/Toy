package com.section.common.content.dto;

public record ContentReactionAnalyticsSummaryRow(
        long totalCount,
        long helpfulCount,
        long notHelpfulCount,
        long uniqueVisitors,
        long evaluatedContentCount
) {
}
