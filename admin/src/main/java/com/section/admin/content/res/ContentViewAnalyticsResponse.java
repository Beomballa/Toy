package com.section.admin.content.res;

import java.util.List;

public record ContentViewAnalyticsResponse(
        String boardType,
        int rangeDays,
        String startDate,
        String endDate,
        String generatedAt,
        Summary summary,
        List<Trend> trend,
        List<TopContent> topContents
) {
    public record Summary(
            long totalViews,
            long uniqueVisitors,
            long viewedContentCount,
            double averageViewsPerContent,
            long previousViews,
            int viewChangeRate
    ) {
    }

    public record Trend(
            String date,
            long viewCount,
            long uniqueVisitors
    ) {
    }

    public record TopContent(
            long documentId,
            String boardType,
            String title,
            long viewCount,
            long uniqueVisitors
    ) {
    }
}
