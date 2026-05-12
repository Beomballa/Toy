package com.section.common.content.dto;

import com.section.common.base.entity.type.YN;
import com.section.common.content.entity.Document;

public record DocumentListQuery(
        Document.BoardType boardType,
        String keyword,
        Document.PublishStatus status,
        YN publicYn,
        Boolean pinnedOnly
) {
}
