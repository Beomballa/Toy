package com.section.front.product.dto;

public record FrontRelatedProductResponse(
        long id,
        String brand,
        String name,
        String reason,
        String model,
        int price,
        int stock,
        String stockStatus,
        String priceLabel,
        String thumbnailUrl
) {
}
