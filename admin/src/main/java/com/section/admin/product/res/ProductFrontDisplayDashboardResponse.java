package com.section.admin.product.res;

import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.AdminFrontDisplayProductQuery;

import java.util.List;

public record ProductFrontDisplayDashboardResponse(
        ProductFrontDisplaySummaryResponse summary,
        List<ProductFrontDisplayListResponse> items,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static ProductFrontDisplayDashboardResponse of(
            AdminFrontDisplayProductQuery query,
            ProductFrontDisplaySummaryResponse summary,
            List<ProductFrontDisplayListResponse> items
    ) {
        return new ProductFrontDisplayDashboardResponse(
                summary,
                items,
                AppliedQuery.from(query),
                ResultMeta.from(query, items.size())
        );
    }

    public record AppliedQuery(
            String keyword,
            String statusCode,
            Long brandNo,
            Long categoryNo,
            String configured,
            String contentStatus,
            boolean featuredOnly,
            boolean lowStockOnly,
            long lowStockThreshold,
            String sortCode
    ) {
        private static AppliedQuery from(AdminFrontDisplayProductQuery query) {
            return new AppliedQuery(
                    query.keyword(),
                    query.status() == null ? null : query.status().name(),
                    query.brandNo(),
                    query.categoryNo(),
                    resolveConfigured(query),
                    query.contentStatus(),
                    query.featuredOnly(),
                    query.lowStockOnly(),
                    query.lowStockThreshold(),
                    query.sort()
            );
        }

        private static String resolveConfigured(AdminFrontDisplayProductQuery query) {
            if (query.configuredOnly()) {
                return "CONFIGURED";
            }
            if (query.unconfiguredOnly()) {
                return "UNCONFIGURED";
            }
            return null;
        }
    }

    public record ResultMeta(
            String resultLabel,
            int filterCount,
            boolean hasActiveFilters,
            String querySignature
    ) {
        private static ResultMeta from(AdminFrontDisplayProductQuery query, int totalCount) {
            return new ResultMeta(
                    hasActiveFilters(query) ? "검색 결과 " + totalCount + "건" : "전체 " + totalCount + "건",
                    countFilters(query),
                    hasActiveFilters(query),
                    buildQuerySignature(query)
            );
        }

        private static int countFilters(AdminFrontDisplayProductQuery query) {
            int count = 0;
            if (query.keyword() != null) count++;
            if (query.status() != null) count++;
            if (query.brandNo() != null) count++;
            if (query.categoryNo() != null) count++;
            if (query.configuredOnly() || query.unconfiguredOnly()) count++;
            if (query.readyContentOnly() || query.incompleteContentOnly()) count++;
            if (query.featuredOnly()) count++;
            if (query.lowStockOnly()) count++;
            return count;
        }

        private static boolean hasActiveFilters(AdminFrontDisplayProductQuery query) {
            return countFilters(query) > 0;
        }

        private static String buildQuerySignature(AdminFrontDisplayProductQuery query) {
            StringBuilder builder = new StringBuilder(sortLabel(query.sort()));
            if (query.keyword() != null) builder.append(" · 검색=").append(query.keyword());
            if (query.status() != null) builder.append(" · 상태=").append(ProductStatus.fromCode(query.status().name()).getDesc());
            if (query.brandNo() != null) builder.append(" · 브랜드=").append(query.brandNo());
            if (query.categoryNo() != null) builder.append(" · 카테고리=").append(query.categoryNo());
            if (query.configuredOnly()) builder.append(" · 노출=설정됨");
            if (query.unconfiguredOnly()) builder.append(" · 노출=미설정");
            if (query.readyContentOnly()) builder.append(" · 문구=완성");
            if (query.incompleteContentOnly()) builder.append(" · 문구=보완필요");
            if (query.featuredOnly()) builder.append(" · Featured");
            if (query.lowStockOnly()) builder.append(" · 저재고<").append(query.lowStockThreshold());
            return builder.toString();
        }

        private static String sortLabel(String sort) {
            if (sort == null) {
                return "Featured 우선";
            }
            return switch (sort) {
                case "LATEST" -> "최신 등록순";
                case "STOCK_ASC" -> "재고 낮은 순";
                case "STOCK_DESC" -> "재고 높은 순";
                case "PRICE_HIGH" -> "발매가 높은 순";
                case "PRICE_LOW" -> "발매가 낮은 순";
                default -> "Featured 우선";
            };
        }
    }
}
