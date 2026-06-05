package com.section.front.product.dto;

import java.util.List;

public record FrontProductResponse(
        long id,
        String brand,
        String category,
        String name,
        String model,
        int price,
        int stock,
        String createdDate,
        String description,
        String mood,
        boolean featured,
        List<FrontProductOptionResponse> options
) {
}
