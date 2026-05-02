package com.section.admin.product.req;

import com.section.common.commerce.dto.ProductListReqDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductListRequest {
//    브랜드, 카테고리, 상태, 검색 조건 = 상품명, 모델
    private Long categoryNo;
    private Long brandNo;
    private String status;
    private String searchKeyword;
    private String orderType;
    private Boolean lowStockOnly;

    public ProductListReqDto toProductListReqDto() {
        ProductListReqDto reqDto = new ProductListReqDto();
        reqDto.setCategoryNo(categoryNo);
        reqDto.setBrandNo(brandNo);
        reqDto.setStatus(status);
        reqDto.setSearchKeyword(searchKeyword);
        reqDto.setOrderType(orderType);
        reqDto.setLowStockOnly(lowStockOnly);
        return reqDto;
    }
}
