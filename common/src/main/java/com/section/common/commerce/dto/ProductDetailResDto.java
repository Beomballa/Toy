package com.section.common.commerce.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
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

    // JPQL 'SELECT new'에서 사용할 생성자
    public ProductDetailResDto(Long productNo, Long categoryNo, String categoryName,
                               Long brandNo, String brandName, String productName,
                               String productModel, Integer releasePrice,
                               LocalDate releaseDt, String thumbnailUrl) {
        this.productNo = productNo;
        this.categoryNo = categoryNo;
        this.categoryName = categoryName;
        this.brandNo = brandNo;
        this.brandName = brandName;
        this.productName = productName;
        this.productModel = productModel;
        this.releasePrice = releasePrice;
        this.releaseDt = releaseDt;
        this.thumbnailUrl = thumbnailUrl;
    }
}