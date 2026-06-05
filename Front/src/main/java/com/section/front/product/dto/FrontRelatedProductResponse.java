package com.section.front.product.dto;

public record FrontRelatedProductResponse(
        long id,
        String brand,
        String name,
        String model,
        int price,
        int stock
) {
}
