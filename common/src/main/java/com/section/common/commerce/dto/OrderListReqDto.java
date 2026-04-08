package com.section.common.commerce.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OrderListReqDto {
    private String status;
    private String searchKeyword;
    private String startDate;
    private String endDate;
}
