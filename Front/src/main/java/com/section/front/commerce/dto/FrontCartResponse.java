package com.section.front.commerce.dto;

import java.util.List;

public record FrontCartResponse(
        List<FrontCartItemResponse> items,
        int itemCount,
        int totalQuantity,
        int totalAmount
) {
    public static FrontCartResponse empty() {
        return new FrontCartResponse(List.of(), 0, 0, 0);
    }
}
