package com.section.common.content.dto;

import com.section.common.content.entity.Document;

public record DocumentListQuery(
        Document.BoardType boardType,
        String keyword
) {
}
