package com.section.common.commerce.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductListReqDto {
    private Long categoryNo;
    private Long brandNo;
    private String status;
    private String searchKeyword;
    private String orderType;
}
