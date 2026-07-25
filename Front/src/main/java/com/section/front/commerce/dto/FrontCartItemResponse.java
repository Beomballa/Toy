package com.section.front.commerce.dto;

public record FrontCartItemResponse(
        long itemId,
        long productId,
        long optionId,
        String productName,
        String optionName,
        String thumbnailUrl,
        int unitPrice,
        int quantity,
        int stock,
        int lineAmount
) {
}
