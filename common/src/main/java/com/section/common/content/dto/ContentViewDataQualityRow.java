package com.section.common.content.dto;

import java.time.LocalDate;

public record ContentViewDataQualityRow(
        long totalEventCount,
        long validEventCount,
        long orphanEventCount,
        LocalDate oldestViewedDate,
        LocalDate latestViewedDate
) {
    public ContentViewDataQualityRow(
            long totalEventCount,
            long validEventCount,
            LocalDate oldestViewedDate,
            LocalDate latestViewedDate
    ) {
        this(
                totalEventCount,
                validEventCount,
                Math.max(0, totalEventCount - validEventCount),
                oldestViewedDate,
                latestViewedDate
        );
    }
}
