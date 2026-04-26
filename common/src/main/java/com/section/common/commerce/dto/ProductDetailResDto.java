package com.section.common.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ProductDetailResDto {
    private Long productNo;
    private Long categoryNo;
    private String categoryName;
    private Long brandNo;
    private String brandName;
    private String productName;
    private String productModel;
    private Integer releasePrice;
    private LocalDate releaseDt;
    private String thumbnailUrl;
    private String status;
    private LocalDateTime crtDtm;
    private LocalDateTime uptDtm;
}
