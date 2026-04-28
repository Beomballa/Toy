package com.section.common.commerce.dto;

import com.section.common.base.entity.type.OrderStatus;

import java.time.LocalDateTime;

public record OrderListQuery(
        OrderStatus status,
        String searchKeyword,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
) {
}
