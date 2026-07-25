package com.section.front.commerce.dto;

import java.util.List;

public record FrontOrderDetailResponse(
        String orderNumber,
        String buyerName,
        int totalAmount,
        String status,
        String statusLabel,
        int statusStep,
        String orderedAt,
        String deliveryCompany,
        String trackingNumber,
        FrontOrderDeliveryResponse delivery,
        List<FrontOrderItemResponse> items,
        List<FrontOrderStatusEventResponse> statusHistory
) {
}
