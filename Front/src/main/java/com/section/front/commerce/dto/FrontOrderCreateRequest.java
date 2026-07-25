package com.section.front.commerce.dto;

public record FrontOrderCreateRequest(
        String buyerName,
        String buyerPhone,
        String recipientName,
        String recipientPhone,
        String postalCode,
        String address1,
        String address2,
        String deliveryRequest
) {
}
