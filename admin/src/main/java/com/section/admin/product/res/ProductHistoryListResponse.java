package com.section.admin.product.res;

import com.section.common.base.entity.type.ProductHistoryActionType;
import com.section.common.commerce.dto.ProductHistoryListQuery;
import com.section.common.commerce.dto.ProductHistoryListResDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public record ProductHistoryListResponse(
        List<Item> items,
        long totalElements,
        int totalPages,
        int pageSize,
        long rangeStart,
        long rangeEnd,
        AppliedQuery appliedQuery
) {
    public static ProductHistoryListResponse of(
            Page<ProductHistoryListResDto> page,
            ProductHistoryListQuery query,
            Map<Long, String> actorNameMap
    ) {
        long rangeStart = page.getTotalElements() == 0 ? 0 : page.getNumber() * page.getSize() + 1L;
        long rangeEnd = page.getTotalElements() == 0 ? 0 : Math.min(page.getTotalElements(), rangeStart + page.getNumberOfElements() - 1L);
        return new ProductHistoryListResponse(
                page.getContent().stream()
                        .map(item -> Item.from(item, actorNameMap.getOrDefault(item.getActorNo(), item.getActorNo() == null ? "-" : "관리자#" + item.getActorNo())))
                        .toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                rangeStart,
                rangeEnd,
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
        public static Item from(ProductHistoryListResDto item, String actorName) {
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
                    actorName,
                    item.getActionDtm() == null ? "-" : item.getActionDtm().toString().replace('T', ' ')
            );
        }
    }

    public record AppliedQuery(
            Long productNo,
            String actionType,
            String keyword,
            String startDate,
            String endDate
    ) {
        public static AppliedQuery from(ProductHistoryListQuery query) {
            return new AppliedQuery(
                    query.productNo(),
                    query.actionType() == null ? null : query.actionType().name(),
                    query.keyword(),
                    query.startDate() == null ? null : query.startDate().toString(),
                    query.endDate() == null ? null : query.endDate().toString()
            );
        }
    }
}
