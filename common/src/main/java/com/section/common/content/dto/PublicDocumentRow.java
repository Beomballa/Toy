package com.section.common.content.dto;

import com.section.common.base.entity.type.YN;
import com.section.common.content.entity.Document;

import java.time.LocalDateTime;

public record PublicDocumentRow(
        Long id,
        Document.BoardType boardType,
        String title,
        String content,
        int viewCount,
        YN pinnedYn,
        LocalDateTime createdAt
) {
}
