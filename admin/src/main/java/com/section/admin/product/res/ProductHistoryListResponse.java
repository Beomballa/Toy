package com.section.admin.product.res;

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
        AppliedQuery appliedQuery
) {
    public static ProductHistoryListResponse of(
            Page<ProductHistoryListResDto> page,
            ProductHistoryListQuery query
    ) {
        long rangeStart = page.getTotalElements() == 0 ? 0 : page.getNumber() * page.getSize() + 1L;
        long rangeEnd = page.getTotalElements() == 0 ? 0 : Math.min(page.getTotalElements(), rangeStart + page.getNumberOfElements() - 1L);
        String pageInfoLabel = page.getTotalElements() == 0
                ? "조회 결과 없음"
                : "%d-%d / %d건 · %d페이지".formatted(rangeStart, rangeEnd, page.getTotalElements(), page.getNumber() + 1);
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
                AppliedQuery.from(query)
        );
    }

    public record Item(
            Long historyNo,
            Long productNo,
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
            return new Item(
                    item.getHistoryNo(),
                    item.getProductNo(),
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
                    query.actorKeyword(),
                    query.startDate() == null ? null : query.startDate().toString(),
                    query.endDate() == null ? null : query.endDate().toString(),
                    query.orderType() == null ? ProductHistoryOrderType.LATEST.getCode() : query.orderType().getCode(),
                    query.orderType() == null ? ProductHistoryOrderType.LATEST.getDesc() : query.orderType().getDesc()
            );
        }
    }
}
