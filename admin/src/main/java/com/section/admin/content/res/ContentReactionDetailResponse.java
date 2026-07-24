package com.section.admin.content.res;

import java.util.List;

public record ContentReactionDetailResponse(
        long documentId,
        int rangeDays,
        String startDate,
        String endDate,
        long totalCount,
        long helpfulCount,
        long notHelpfulCount,
        int helpfulRate,
        long recentActivityCount,
        String status,
        String statusMessage,
        List<Trend> trend
) {
    public record Trend(
            String date,
            long totalCount,
            long helpfulCount,
            long notHelpfulCount
    ) {
    }
}
