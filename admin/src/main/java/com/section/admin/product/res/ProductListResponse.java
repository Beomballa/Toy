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
        AppliedQueryItem appliedQuery,
        ResultMetaItem resultMeta
) {


    public static ProductListResponse of(
            Page<ProductListItem> page,
            ProductStatsItem stats,
            AppliedQueryItem appliedQuery,
            ResultMetaItem resultMeta
    ){
        return new ProductListResponse(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                stats,
                appliedQuery,
                resultMeta
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

    public record ResultMetaItem(
            String resultLabel,
            String pageInfoLabel,
            String orderTypeLabel,
            int pageSize,
            long rangeStart,
            long rangeEnd,
            long appliedFilterCount,
            boolean hasActiveFilters,
            String querySignature
    ) {
        public static ResultMetaItem from(ProductListQuery query, Page<?> page) {
            long totalElements = page.getTotalElements();
            int totalPages = page.getTotalPages();
            boolean hasActiveFilters = hasActiveFilters(query);
            long appliedFilterCount = appliedFilterCount(query);
            String resultLabel = hasActiveFilters
                    ? String.format("검색 결과 %,d개", totalElements)
                    : String.format("전체 %,d개", totalElements);
            String pageInfoLabel = totalElements == 0
                    ? "조건에 맞는 상품이 없습니다."
                    : String.format("%s / %d페이지", resultLabel, Math.max(totalPages, 1));
            long rangeStart = totalElements == 0 ? 0 : (long) page.getNumber() * page.getSize() + 1;
            long rangeEnd = totalElements == 0 ? 0 : Math.min(totalElements, rangeStart + page.getNumberOfElements() - 1);

            return new ResultMetaItem(
                    resultLabel,
                    pageInfoLabel,
                    orderTypeLabel(query.orderType()),
                    page.getSize(),
                    rangeStart,
                    rangeEnd,
                    appliedFilterCount,
                    hasActiveFilters,
                    querySignature(query)
            );
        }

        private static boolean hasActiveFilters(ProductListQuery query) {
            return query.brandNo() != null
                    || query.categoryNo() != null
                    || query.status() != null
                    || query.lowStockOnly()
                    || query.createdTodayOnly()
                    || query.searchKeyword() != null;
        }

        private static long appliedFilterCount(ProductListQuery query) {
            long count = 0;
            if (query.brandNo() != null) count++;
            if (query.categoryNo() != null) count++;
            if (query.status() != null) count++;
            if (query.lowStockOnly()) count++;
            if (query.createdTodayOnly()) count++;
            if (query.searchKeyword() != null) count++;
            return count;
        }

        private static String orderTypeLabel(ProductOrderType orderType) {
            if (orderType == null) {
                return "최신순";
            }

            return switch (orderType) {
                case RECENT -> "최신순";
                case RELEASE_PRICE -> "발매가순";
                case STOCK_COUNT -> "재고순";
            };
        }

        private static String querySignature(ProductListQuery query) {
            StringBuilder builder = new StringBuilder(orderTypeLabel(query.orderType()));
            if (query.searchKeyword() != null) {
                builder.append(" · 검색=").append(query.searchKeyword());
            }
            if (query.status() != null) {
                builder.append(" · 상태=").append(query.status().name());
            }
            if (query.lowStockOnly()) {
                builder.append(" · 재고<").append(query.effectiveLowStockThreshold());
            }
            if (query.createdTodayOnly()) {
                builder.append(" · 오늘등록");
            }
            return builder.toString();
        }
    }
}
