package com.section.common.commerce.dto;

import com.section.common.base.entity.type.ProductHistoryActionType;

import java.time.LocalDate;

public record ProductHistoryListQuery(
        Long productNo,
        ProductHistoryActionType actionType,
        String keyword,
        LocalDate startDate,
        LocalDate endDate
) {
}
