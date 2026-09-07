package com.section.front.commerce.dto;

public record FrontMemberOrderClaimItemResponse(
        long claimNo,
        String orderNumber,
        String claimType,
        String claimTypeLabel,
        String status,
        String statusLabel,
        String reason,
        String requestedAt
) {
}
