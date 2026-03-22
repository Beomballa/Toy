package com.section.common.commerce.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProductListResDto {
    private Long productNo;
    private String productName;
    private String thumbnailUrl;
    private String productModel;
    private String brandName;
    private Integer releasePrice;
    private Long totalStock;
    private String status;
    private LocalDateTime crtDtm;
}
