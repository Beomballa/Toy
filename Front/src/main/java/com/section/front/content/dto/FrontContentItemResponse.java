package com.section.front.content.dto;

public record FrontContentItemResponse(
        long id,
        String boardType,
        String title,
        String summary,
        int viewCount,
        boolean pinned,
        String createdDate
) {
}
