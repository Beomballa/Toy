package com.section.front.content.dto;

public record FrontContentNavigationResponse(
        long id,
        String boardType,
        String title,
        String createdDate
) {
}
