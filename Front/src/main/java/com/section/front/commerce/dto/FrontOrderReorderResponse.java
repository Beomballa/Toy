package com.section.front.commerce.dto;

import java.util.List;

public record FrontOrderReorderResponse(
        FrontCartResponse cart,
        int addedCount,
        List<String> unavailableProducts
) {
}
