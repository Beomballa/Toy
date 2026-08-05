package com.section.front.memberactivity.dto;

import com.section.front.product.dto.FrontProductResponse;

public record FrontMemberActivityProductResponse(
        long id,
        String brand,
        String category,
        String name,
        String headline,
        String model,
        int price,
        int stock,
        String thumbnailUrl
) {
    public static FrontMemberActivityProductResponse from(FrontProductResponse product) {
        return new FrontMemberActivityProductResponse(
                product.id(),
                product.brand(),
                product.category(),
                product.name(),
                product.headline(),
                product.model(),
                product.price(),
                product.stock(),
                product.thumbnailUrl()
        );
    }
}
