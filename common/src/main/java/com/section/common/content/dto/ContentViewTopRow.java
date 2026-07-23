package com.section.common.content.dto;

import com.section.common.content.entity.Document;

public record ContentViewTopRow(
        long documentId,
        Document.BoardType boardType,
        String title,
        long viewCount,
        long uniqueVisitors
) {
}
