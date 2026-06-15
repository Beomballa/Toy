package com.section.common.content.dto;

import com.section.common.base.entity.type.YN;
import com.section.common.content.entity.Document;

import java.time.LocalDateTime;

public record DocumentListQuery(
        Document.BoardType boardType,
        String keyword,
        Document.PublishStatus status,
        YN publicYn,
        Boolean pinnedOnly,
        Long productNo,
        Boolean productLinked,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {
}
