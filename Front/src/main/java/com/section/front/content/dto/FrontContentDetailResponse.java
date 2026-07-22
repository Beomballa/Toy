package com.section.front.content.dto;

import java.util.List;

public record FrontContentDetailResponse(
        long id,
        String boardType,
        String title,
        String content,
        int viewCount,
        boolean pinned,
        String createdDate,
        List<FrontContentItemResponse> relatedContents
) {
}
