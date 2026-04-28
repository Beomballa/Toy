package com.section.common.commerce.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderListItemDto {
    private Long orderNo;
    private String orderNum;
    private String buyerName;
    private String buyerPhone;
    private Integer totalAmount;
    private String status;
    private LocalDateTime crtDtm;
    private String firstProductName;
    private Long itemCount;
}
