package com.section.front.commerce.dto;

public record FrontMemberOrderItemResponse(
        String orderNumber,
        String productName,
        int itemCount,
        int totalAmount,
        String status,
        String statusLabel,
        String orderedAt
) {
}
