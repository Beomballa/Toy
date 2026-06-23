package com.section.common.commerce.dto;

import com.section.common.base.entity.type.ProductHistoryActionType;
import com.section.common.base.entity.type.ProductHistoryOrderType;

import java.time.LocalDate;

public record ProductHistoryListQuery(
        Long productNo,
        ProductHistoryActionType actionType,
        String keyword,
        Long actorNo,
        String actorKeyword,
        LocalDate startDate,
        LocalDate endDate,
        ProductHistoryOrderType orderType
) {
}
