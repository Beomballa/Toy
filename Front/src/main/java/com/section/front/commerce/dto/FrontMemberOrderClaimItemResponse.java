package com.section.front.commerce.dto;

public record FrontMemberOrderClaimItemResponse(
        String orderNumber,
        String claimType,
        String claimTypeLabel,
        String status,
        String statusLabel,
        String reason,
        String requestedAt
) {
}
