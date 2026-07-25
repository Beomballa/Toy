package com.section.front.commerce.dto;

public record FrontCartItemRequest(
        long productId,
        long optionId,
        int quantity
) {
}
