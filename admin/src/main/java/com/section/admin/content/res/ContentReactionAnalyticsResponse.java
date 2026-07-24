package com.section.admin.content.res;

import java.util.List;

public record ContentReactionAnalyticsResponse(
        String boardType,
        int rangeDays,
        String startDate,
        String endDate,
        String generatedAt,
        String metricBasis,
        Summary summary,
        List<Trend> trend,
        List<Content> topContents,
        List<Content> improvementContents
) {
    public record Summary(
            long totalCount,
            long helpfulCount,
            long notHelpfulCount,
            int helpfulRate,
            long uniqueVisitors,
            long evaluatedContentCount
    ) {
    }

    public record Trend(
            String date,
            long totalCount,
            long helpfulCount,
            long notHelpfulCount,
            int helpfulRate
    ) {
    }

    public record Content(
            long documentId,
            String boardType,
            String title,
            long totalCount,
            long helpfulCount,
            long notHelpfulCount,
            int helpfulRate
    ) {
    }
}
