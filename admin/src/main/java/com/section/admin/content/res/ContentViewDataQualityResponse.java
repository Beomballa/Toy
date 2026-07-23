package com.section.admin.content.res;

public record ContentViewDataQualityResponse(
        long totalEventCount,
        long validEventCount,
        long orphanEventCount,
        String oldestViewedDate,
        String latestViewedDate,
        String status,
        String generatedAt
) {
}
