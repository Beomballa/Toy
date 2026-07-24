package com.section.common.content.dto;

import java.time.LocalDateTime;

public record ContentReactionDataQualityRow(
        long totalCount,
        long validCount,
        LocalDateTime oldestReactedAt,
        LocalDateTime latestReactedAt
) {
    public long orphanCount() {
        return totalCount - validCount;
    }
}
