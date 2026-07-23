package com.section.front.content.dto;

public record FrontPopularContentResponse(
        long id,
        String boardType,
        String title,
        String summary,
        long recentViewCount,
        long uniqueVisitors,
        boolean pinned,
        String createdDate
) {
}
