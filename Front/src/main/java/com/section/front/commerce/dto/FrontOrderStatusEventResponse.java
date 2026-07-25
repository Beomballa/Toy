package com.section.front.commerce.dto;

public record FrontOrderStatusEventResponse(
        String status,
        String statusLabel,
        String changedAt
) {
}
