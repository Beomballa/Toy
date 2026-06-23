package com.section.admin.product.res;

import com.section.admin.log.res.AdminLogSourceLinkSupport;
import com.section.common.base.entity.type.ProductHistoryActionType;
import com.section.common.base.entity.type.ProductHistoryOrderType;
import com.section.common.commerce.dto.ProductHistoryListQuery;
import com.section.common.commerce.dto.ProductHistoryListResDto;
import org.springframework.data.domain.Page;

import java.util.List;

public record ProductHistoryListResponse(
        List<Item> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize,
        long rangeStart,
        long rangeEnd,
        String pageInfoLabel,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static ProductHistoryListResponse of(
            Page<ProductHistoryListResDto> page,
            ProductHistoryListQuery query
    ) {
        long rangeStart = page.getTotalElements() == 0 ? 0 : page.getNumber() * page.getSize() + 1L;
        long rangeEnd = page.getTotalElements() == 0 ? 0 : Math.min(page.getTotalElements(), rangeStart + page.getNumberOfElements() - 1L);
        String pageInfoLabel = page.getTotalElements() == 0
                ? "조회 결과 없음"
                : "%d-%d / %d건 · %d페이지".formatted(rangeStart, rangeEnd, page.getTotalElements(), Math.max(page.getTotalPages(), 1));
        return new ProductHistoryListResponse(
                page.getContent().stream()
                        .map(Item::from)
                        .toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                rangeStart,
                rangeEnd,
                pageInfoLabel,
                AppliedQuery.from(query),
                ResultMeta.from(page, query, rangeStart, rangeEnd, pageInfoLabel)
        );
    }

    public record Item(
            Long historyNo,
            Long productNo,
            Long relatedProductNo,
            String relatedProductLabel,
            String activityLogPath,
            String activityLogLabel,
            String actionType,
            String actionLabel,
            String summary,
            String statusSnapshot,
            Integer optionCount,
            Long totalStock,
            Long actorNo,
            String actorName,
            String actionDtm
    ) {
        public static Item from(ProductHistoryListResDto item) {
            ProductHistoryActionType actionType = ProductHistoryActionType.valueOf(item.getActionType());
            Long relatedProductNo = ProductHistoryRelatedProductSupport.resolveRelatedProductNo(item.getSummary());
            return new Item(
                    item.getHistoryNo(),
                    item.getProductNo(),
                    relatedProductNo,
                    ProductHistoryRelatedProductSupport.resolveRelatedProductLabel(item.getSummary()),
                    AdminLogSourceLinkSupport.resolveProductHistoryLogPath(item.getProductNo(), actionType.name()),
                    "활동 로그 보기",
                    actionType.name(),
                    actionType.getDesc(),
                    item.getSummary(),
                    item.getStatusSnapshot(),
                    item.getOptionCount(),
                    item.getTotalStock(),
                    item.getActorNo(),
                    resolveActorName(item),
                    item.getActionDtm() == null ? "-" : item.getActionDtm().toString().replace('T', ' ')
            );
        }

        private static String resolveActorName(ProductHistoryListResDto item) {
            if (item.getActorName() != null && !item.getActorName().isBlank()) {
                return item.getActorName();
            }
            return item.getActorNo() == null ? "-" : "관리자#" + item.getActorNo();
        }
    }

    public record AppliedQuery(
            Long productNo,
            String actionType,
            String keyword,
            Long actorNo,
            String actorKeyword,
            String startDate,
            String endDate,
            String orderType,
            String orderTypeLabel
    ) {
        public static AppliedQuery from(ProductHistoryListQuery query) {
            return new AppliedQuery(
                    query.productNo(),
                    query.actionType() == null ? null : query.actionType().name(),
                    query.keyword(),
                    query.actorNo(),
                    query.actorKeyword(),
                    query.startDate() == null ? null : query.startDate().toString(),
                    query.endDate() == null ? null : query.endDate().toString(),
                    query.orderType() == null ? ProductHistoryOrderType.LATEST.getCode() : query.orderType().getCode(),
                    query.orderType() == null ? ProductHistoryOrderType.LATEST.getDesc() : query.orderType().getDesc()
            );
        }
    }

    public record ResultMeta(
            String resultLabel,
            String pageInfoLabel,
            int filterCount,
            String querySignature
    ) {
        public static ResultMeta from(
                Page<ProductHistoryListResDto> page,
                ProductHistoryListQuery query,
                long rangeStart,
                long rangeEnd,
                String pageInfoLabel
        ) {
            return new ResultMeta(
                    page.getTotalElements() == 0 ? "조회 결과 없음" : "검색 결과 " + page.getTotalElements() + "건",
                    pageInfoLabel,
                    countFilters(query),
                    buildQuerySignature(query, rangeStart, rangeEnd)
            );
        }

        private static int countFilters(ProductHistoryListQuery query) {
            int count = 0;
            if (query.productNo() != null) count += 1;
            if (query.actionType() != null) count += 1;
            if (query.keyword() != null && !query.keyword().isBlank()) count += 1;
            if (query.actorNo() != null) count += 1;
            if (query.actorKeyword() != null && !query.actorKeyword().isBlank()) count += 1;
            if (query.startDate() != null) count += 1;
            if (query.endDate() != null) count += 1;
            if (query.orderType() != null && query.orderType() != ProductHistoryOrderType.LATEST) count += 1;
            return count;
        }

        private static String buildQuerySignature(ProductHistoryListQuery query, long rangeStart, long rangeEnd) {
            StringBuilder builder = new StringBuilder();
            builder.append(rangeStart).append("-").append(rangeEnd);
            if (query.productNo() != null) builder.append(" · 상품=").append(query.productNo());
            if (query.actionType() != null) builder.append(" · 작업=").append(query.actionType().name());
            if (query.keyword() != null && !query.keyword().isBlank()) builder.append(" · 검색=").append(query.keyword());
            if (query.actorNo() != null) builder.append(" · 작업자번호=").append(query.actorNo());
            if (query.actorKeyword() != null && !query.actorKeyword().isBlank()) builder.append(" · 작업자=").append(query.actorKeyword());
            if (query.orderType() != null && query.orderType() != ProductHistoryOrderType.LATEST) builder.append(" · 정렬=").append(query.orderType().getDesc());
            return builder.toString();
        }
    }
}
