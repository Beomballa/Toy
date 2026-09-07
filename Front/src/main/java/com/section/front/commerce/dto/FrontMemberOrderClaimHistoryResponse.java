package com.section.front.commerce.dto;

public record FrontMemberOrderClaimHistoryResponse(
        String beforeStatusLabel,
        String afterStatusLabel,
        String memo,
        String changedAt
) {
}
