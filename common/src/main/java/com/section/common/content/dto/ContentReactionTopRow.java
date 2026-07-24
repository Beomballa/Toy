package com.section.common.content.dto;

import com.section.common.content.entity.Document;

public record ContentReactionTopRow(
        long documentId,
        Document.BoardType boardType,
        String title,
        long helpfulCount,
        long notHelpfulCount
) {
    public long totalCount() {
        return helpfulCount + notHelpfulCount;
    }
}
