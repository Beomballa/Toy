package com.section.common.content.dto;

import com.section.common.content.entity.Document;

public record DocumentDailyStatsRow(
        Document.BoardType boardType,
        Long totalCount,
        Long publishedCount,
        Long draftCount,
        Long publicCount,
        Long privateCount,
        Long pinnedCount,
        Long linkedCount,
        Long totalViewCount
) {
    public DocumentDailyStatsRow {
        totalCount = defaultZero(totalCount);
        publishedCount = defaultZero(publishedCount);
        draftCount = defaultZero(draftCount);
        publicCount = defaultZero(publicCount);
        privateCount = defaultZero(privateCount);
        pinnedCount = defaultZero(pinnedCount);
        linkedCount = defaultZero(linkedCount);
        totalViewCount = defaultZero(totalViewCount);
    }

    public static DocumentDailyStatsRow empty(Document.BoardType boardType) {
        return new DocumentDailyStatsRow(boardType, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    private static long defaultZero(Long value) {
        return value == null ? 0L : value;
    }
}
