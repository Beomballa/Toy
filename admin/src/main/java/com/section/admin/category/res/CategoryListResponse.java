package com.section.admin.category.res;

import com.section.admin.category.req.CategoryListRequest;

import java.util.List;

public record CategoryListResponse(
        List<CategoryResponse> items,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static CategoryListResponse of(List<CategoryResponse> items, CategoryListRequest request) {
        return new CategoryListResponse(
                items,
                new AppliedQuery(request.normalizedKeyword(), request.normalizedIsActive(), request.getDepth()),
                ResultMeta.from(items.size(), request)
        );
    }

    public record AppliedQuery(
            String keyword,
            String isActive,
            Integer depth
    ) {
    }

    public record ResultMeta(
            String resultLabel,
            long appliedFilterCount,
            boolean hasActiveFilters,
            String querySignature
    ) {
        private static ResultMeta from(int size, CategoryListRequest request) {
            long filterCount = appliedFilterCount(request);
            boolean hasActiveFilters = filterCount > 0;
            return new ResultMeta(
                    hasActiveFilters ? String.format("검색 결과 %d건", size) : String.format("전체 %d건", size),
                    filterCount,
                    hasActiveFilters,
                    querySignature(request)
            );
        }

        private static long appliedFilterCount(CategoryListRequest request) {
            long count = 0;
            if (request.normalizedKeyword() != null) count++;
            if (request.normalizedIsActive() != null) count++;
            return count;
        }

        private static String querySignature(CategoryListRequest request) {
            StringBuilder builder = new StringBuilder(request.getDepth() != null && request.getDepth() == 2 ? "중분류 기준" : "대분류 기준");
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
