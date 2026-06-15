package com.section.admin.content.res;

import com.section.common.content.dto.DocumentSummaryDto;

public record ContentSummaryResponse(
        long totalCount,
        long publishedCount,
        long draftCount,
        long publicCount,
        long privateCount,
        long pinnedCount,
        long linkedCount,
        long totalViewCount
) {
    public static ContentSummaryResponse from(DocumentSummaryDto summary) {
        return new ContentSummaryResponse(
                summary.totalCount(),
                summary.publishedCount(),
                summary.draftCount(),
                summary.publicCount(),
                summary.privateCount(),
                summary.pinnedCount(),
                summary.linkedCount(),
                summary.totalViewCount()
        );
    }
}
