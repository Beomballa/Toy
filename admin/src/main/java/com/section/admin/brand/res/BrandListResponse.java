package com.section.admin.brand.res;

import com.section.admin.brand.req.BrandListRequest;

import java.util.List;

public record BrandListResponse(
        List<BrandResponse> items,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static BrandListResponse of(List<BrandResponse> items, BrandListRequest request) {
        return new BrandListResponse(
                items,
                new AppliedQuery(request.normalizedKeyword(), request.normalizedIsActive()),
                ResultMeta.from(items.size(), request)
        );
    }

    public record AppliedQuery(
            String keyword,
            String isActive
    ) {
    }

    public record ResultMeta(
            String resultLabel,
            long appliedFilterCount,
            boolean hasActiveFilters,
            String querySignature
    ) {
        private static ResultMeta from(int size, BrandListRequest request) {
            long filterCount = appliedFilterCount(request);
            boolean hasActiveFilters = filterCount > 0;
            return new ResultMeta(
                    hasActiveFilters ? String.format("검색 결과 %d건", size) : String.format("전체 %d건", size),
                    filterCount,
                    hasActiveFilters,
                    querySignature(request)
            );
        }

        private static long appliedFilterCount(BrandListRequest request) {
            long count = 0;
            if (request.normalizedKeyword() != null) count++;
            if (request.normalizedIsActive() != null) count++;
            return count;
        }

        private static String querySignature(BrandListRequest request) {
            StringBuilder builder = new StringBuilder("브랜드명 기준");
            if (request.normalizedKeyword() != null) {
                builder.append(" · 검색=").append(request.normalizedKeyword());
            }
            if (request.normalizedIsActive() != null) {
                builder.append(" · 상태=").append("Y".equalsIgnoreCase(request.normalizedIsActive()) ? "사용" : "중지");
            }
            return builder.toString();
        }
    }
}
