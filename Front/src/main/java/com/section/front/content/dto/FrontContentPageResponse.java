package com.section.front.content.dto;

import java.util.List;

public record FrontContentPageResponse(
        List<FrontContentItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
