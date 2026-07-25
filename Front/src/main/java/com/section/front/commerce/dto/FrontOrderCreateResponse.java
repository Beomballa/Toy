package com.section.front.commerce.dto;

public record FrontOrderCreateResponse(
        long orderId,
        String orderNumber,
        int totalAmount,
        String status
) {
}
