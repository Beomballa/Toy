package com.section.common.content.dto;

import com.section.common.content.entity.Document;

public record ContentReactionDocumentTotalRow(
        long documentId,
        Document.BoardType boardType,
        String title,
        long totalCount
) {
}
