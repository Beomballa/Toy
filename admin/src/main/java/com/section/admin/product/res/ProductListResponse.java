package com.section.admin.product.res;

import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductStatsDto;
import com.section.common.util.DateUtil;
import lombok.*;
import org.springframework.data.domain.Page;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Setter
@Builder
public class ProductListResponse {

    private Page<ProductListItem> products;
    private ProductStatsItem productStats;

    public static ProductListResponse of(Page<ProductListItem> products, ProductStatsItem stats){
        return ProductListResponse.builder()
                .products(products)
                .productStats(stats)
                .build();
    }

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

    @Getter @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductStatsItem{
        private Long totalCount;
        private Long activeCount;
        private Long lowStockCount;
        private Long todayCount;

        public static ProductStatsItem from(ProductStatsDto resDto){
            if(resDto == null) return new ProductStatsItem();
            return ProductStatsItem.builder()
                    .totalCount(resDto.getTotalCount())
                    .activeCount(resDto.getActiveCount())
                    .lowStockCount(resDto.getLowStockCount())
                    .todayCount(resDto.getTodayCount())
                    .build();
        }
    }
}
