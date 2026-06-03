package com.section.common.commerce.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusSummaryDto {
    private String status;
    private Long count;
}
