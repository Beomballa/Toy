package com.section.common.commerce.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStatsDto {
    private Long totalCount;
    private Long activeCount;
    private Long lowStockCount;
    private Long todayCount;
}