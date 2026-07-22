package com.section.front.content.dto;

import java.util.List;

public record FrontContentHighlightsResponse(
        List<FrontContentItemResponse> notices,
        List<FrontContentItemResponse> styles
) {
}
