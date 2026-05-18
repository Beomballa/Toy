package com.section.admin.category.res;

import com.section.admin.category.req.CategoryListRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public record CategoryListResponse(
        List<CategoryResponse> items,
        int currentPage,
        int totalPages,
        long totalElements,
        int pageSize,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static CategoryListResponse of(Page<CategoryResponse> page, CategoryListRequest request) {
        return new CategoryListResponse(
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                new AppliedQuery(request.normalizedKeyword(), request.normalizedIsActive(), request.getDepth()),
                ResultMeta.from(page, request)
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
            String pageInfoLabel,
            long appliedFilterCount,
            boolean hasActiveFilters,
            String querySignature,
            long rangeStart,
            long rangeEnd
    ) {
        private static ResultMeta from(Page<CategoryResponse> page, CategoryListRequest request) {
            long filterCount = appliedFilterCount(request);
            boolean hasActiveFilters = filterCount > 0;
            long totalElements = page.getTotalElements();
            long rangeStart = totalElements == 0 ? 0 : (long) page.getNumber() * page.getSize() + 1;
            long rangeEnd = totalElements == 0 ? 0 : rangeStart + page.getNumberOfElements() - 1;
            String resultLabel = hasActiveFilters
                    ? String.format("검색 결과 %d건", totalElements)
                    : String.format("전체 %d건", totalElements);
            return new ResultMeta(
                    resultLabel,
                    totalElements == 0
                            ? "조건에 맞는 카테고리가 없습니다."
                            : String.format("%d-%d / %d건 · %d페이지", rangeStart, rangeEnd, totalElements, Math.max(page.getTotalPages(), 1)),
                    filterCount,
                    hasActiveFilters,
                    querySignature(request),
                    rangeStart,
                    rangeEnd
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
