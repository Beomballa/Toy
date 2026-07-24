package com.section.admin.content.res;

import java.util.List;

public record ContentPerformanceAnalyticsResponse(
        String boardType,
        int rangeDays,
        String startDate,
        String endDate,
        String generatedAt,
        Summary summary,
        List<Content> priorityContents
) {
    public record Summary(
            long totalViews,
            long totalReactions,
            int helpfulRate,
            int reactionCoverageRate,
            long analyzedContentCount,
            long actionRequiredCount
    ) {
    }

    public record Content(
            long documentId,
            String boardType,
            String title,
            long viewCount,
            long uniqueVisitors,
            long reactionCount,
            long helpfulCount,
            long notHelpfulCount,
            int helpfulRate,
            int reactionCoverageRate,
            int priorityScore,
            String status,
            String statusMessage,
            Long operationTaskNo,
            String operationTaskPath
    ) {
    }
}
