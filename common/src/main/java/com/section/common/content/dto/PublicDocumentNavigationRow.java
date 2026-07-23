package com.section.common.content.dto;

import com.section.common.content.entity.Document;

import java.time.LocalDateTime;

public record PublicDocumentNavigationRow(
        Long id,
        Document.BoardType boardType,
        String title,
        LocalDateTime createdAt
) {
}
