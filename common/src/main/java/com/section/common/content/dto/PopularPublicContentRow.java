package com.section.common.content.dto;

import com.section.common.base.entity.type.YN;
import com.section.common.content.entity.Document;

import java.time.LocalDateTime;

public record PopularPublicContentRow(
        Long id,
        Document.BoardType boardType,
        String title,
        String content,
        long recentViewCount,
        long uniqueVisitors,
        YN pinnedYn,
        LocalDateTime createdAt
) {
}
