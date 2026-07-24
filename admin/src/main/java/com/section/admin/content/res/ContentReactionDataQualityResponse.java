package com.section.admin.content.res;

public record ContentReactionDataQualityResponse(
        long totalCount,
        long validCount,
        long orphanCount,
        String oldestReactedAt,
        String latestReactedAt,
        String status,
        String checkedAt
) {
}
