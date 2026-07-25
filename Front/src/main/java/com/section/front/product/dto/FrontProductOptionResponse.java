package com.section.front.product.dto;

public record FrontProductOptionResponse(
        Long id,
        String name,
        int stock,
        int additionalPrice
) {
    public FrontProductOptionResponse(String name, int stock) {
        this(null, name, stock, 0);
    }
}
