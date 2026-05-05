package com.section.admin.product.res;

import com.section.common.base.entity.type.ProductOrderType;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductListQuery;
import com.section.common.commerce.dto.ProductStatsDto;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.util.DateUtil;
import lombok.*;
import org.springframework.data.domain.Page;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public record ProductListResponse(
        List<ProductListItem> products,
        int currentPage,
        int totalPages,
        Long totalElements,
        ProductStatsItem productStats,
        AppliedQueryItem appliedQuery
) {


    public static ProductListResponse of(Page<ProductListItem> page, ProductStatsItem stats, AppliedQueryItem appliedQuery){
        return new ProductListResponse(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                stats,
                appliedQuery
        );
    }

    // ──────────────────────────────────────────
    // 개별 상품 아이템
    // ──────────────────────────────────────────
    public record ProductListItem(
            Long productNo,
            String productName,
            String thumbnailUrl,
            String productModel,
            String brandName,
            String releasePrice,
            Long totalStock,
            String statusCode,
            String statusDesc,
            String crtDtm
    ){
        public static ProductListItem from(ProductListResDto resDto) {
            ProductStatus status = ProductStatus.fromCode(resDto.getStatus());
            return new ProductListItem(
                    resDto.getProductNo(),
                    resDto.getProductName(),
                    resDto.getThumbnailUrl(),
                    resDto.getProductModel(),
                    resDto.getBrandName(),
                    String.format("%,d원", resDto.getReleasePrice()),
                    resDto.getTotalStock(),
                    status.name(),
                    status.getDesc(),
                    resDto.getCrtDtm() != null ? DateUtil.localDateTimeToStr(resDto.getCrtDtm()) : ""
            );
        }
    }

    public record ProductStatsItem(
            Long totalCount,
            Long activeCount,
            Long lowStockCount,
            Long todayCount,
            Long lowStockThreshold
    ){
        public static ProductStatsItem empty() {
            return new ProductStatsItem(0L, 0L, 0L, 0L, 100L);
        }

        public static ProductStatsItem from(ProductStatsDto resDto, Long lowStockThreshold){
            return Optional.ofNullable(resDto)
                    .map(dto -> new ProductStatsItem(
                            dto.getTotalCount(),
                            dto.getActiveCount(),
                            dto.getLowStockCount(),
                            dto.getTodayCount(),
                            lowStockThreshold == null ? 100L : lowStockThreshold
                    ))
                    .orElseGet(ProductStatsItem::empty);
        }
    }

    public record AppliedQueryItem(
            Long categoryNo,
            Long brandNo,
            String statusCode,
            String searchKeyword,
            String orderTypeCode,
            boolean lowStockOnly,
            Long lowStockThreshold,
            boolean createdTodayOnly
    ) {
        public static AppliedQueryItem from(ProductListQuery query) {
            return new AppliedQueryItem(
                    query.categoryNo(),
                    query.brandNo(),
                    query.status() == null ? null : query.status().name(),
                    query.searchKeyword(),
                    orderTypeCode(query.orderType()),
                    query.lowStockOnly(),
                    query.lowStockThreshold(),
                    query.createdTodayOnly()
            );
        }

        private static String orderTypeCode(ProductOrderType orderType) {
            if (orderType == null) {
                return "r";
            }

            return switch (orderType) {
                case RECENT -> "r";
                case RELEASE_PRICE -> "p";
                case STOCK_COUNT -> "c";
            };
        }
    }
}
