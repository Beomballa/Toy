package com.section.common.commerce.dto;

import com.section.common.base.entity.type.OrderHistoryOrderType;

import java.time.LocalDate;

public record OrderHistoryListQuery(
        Long orderNo,
        String actionType,
        String keyword,
        String actorKeyword,
        LocalDate startDate,
        LocalDate endDate,
        OrderHistoryOrderType orderType
) {
}
