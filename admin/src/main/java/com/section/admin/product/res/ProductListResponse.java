package com.section.admin.product.res;

import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.util.DateUtil;
import lombok.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Setter
public class ProductListResponse {
    private List<ProductListResponse> productList;


    @Getter @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductListItem { // 개별 상품 정보 아이템
        private Long productNo;
        private String productName;
        private String thumbnailUrl;
        private String productModel;
        private String brandName;
        private String releasePrice;
        private Long totalStock;
        private String status;
        private String crtDtm;

        public static ProductListItem from(ProductListResDto resDto) {
            return ProductListItem.builder()
                    .productNo(resDto.getProductNo())
                    .productName(resDto.getProductName())
                    .thumbnailUrl(resDto.getThumbnailUrl())
                    .productModel(resDto.getProductModel())
                    .brandName(resDto.getBrandName())
                    .releasePrice(String.format("%,d원", resDto.getReleasePrice()))
                    .totalStock(resDto.getTotalStock() != null ? resDto.getTotalStock() : 0L)
                    .status(resDto.getStatus())
                    .crtDtm(resDto.getCrtDtm() != null ? DateUtil.localDateTimeToStr(resDto.getCrtDtm()) : "")
                    .build();
        }
    }
}
