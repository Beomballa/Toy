package com.section.front.commerce.dto;

public record FrontOrderItemResponse(
        long productId,
        String productName,
        String thumbnailUrl,
        int unitPrice,
        int quantity,
        int lineAmount
) {
}
