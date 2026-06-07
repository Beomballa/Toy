package com.section.front.product.dto;

import java.util.List;

public record FrontProductResponse(
        long id,
        String brand,
        String category,
        String name,
        String headline,
        String model,
        int price,
        int stock,
        String createdDate,
        String description,
        String mood,
        boolean featured,
        Integer featuredRank,
        String stockStatus,
        String priceLabel,
        List<FrontProductOptionResponse> options
) {
}
