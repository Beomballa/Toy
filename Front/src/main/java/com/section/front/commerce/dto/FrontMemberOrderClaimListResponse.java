package com.section.front.commerce.dto;

import java.util.List;

public record FrontMemberOrderClaimListResponse(
        List<FrontMemberOrderClaimItemResponse> items,
        int page,
        boolean hasNext
) {
}
