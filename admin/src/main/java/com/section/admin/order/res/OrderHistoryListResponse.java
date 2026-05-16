package com.section.admin.order.res;

import com.section.admin.order.support.OrderViewFormatter;
import com.section.common.base.entity.type.OrderHistoryOrderType;
import com.section.common.commerce.dto.OrderHistoryListQuery;
import com.section.common.commerce.dto.OrderHistoryListResDto;
import org.springframework.data.domain.Page;

import java.util.List;

public record OrderHistoryListResponse(
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
    public static OrderHistoryListResponse of(Page<OrderHistoryListResDto> page, OrderHistoryListQuery query) {
        long rangeStart = page.getTotalElements() == 0 ? 0 : page.getNumber() * page.getSize() + 1L;
        long rangeEnd = page.getTotalElements() == 0 ? 0 : Math.min(page.getTotalElements(), rangeStart + page.getNumberOfElements() - 1L);
        String pageInfoLabel = page.getTotalElements() == 0
                ? "조회 결과 없음"
                : "%d-%d / %d건 · %d페이지".formatted(rangeStart, rangeEnd, page.getTotalElements(), page.getNumber() + 1);

        return new OrderHistoryListResponse(
                page.getContent().stream().map(Item::from).toList(),
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
            Long orderNo,
            String actionType,
            String actionLabel,
            String beforeStatusDesc,
            String afterStatusDesc,
            String reason,
            String adminMemoSnapshot,
            String deliveryCompany,
            String trackingNum,
            Long actorNo,
            String actorName,
            String actionDtm
    ) {
        public static Item from(OrderHistoryListResDto item) {
            return new Item(
                    item.getHistoryNo(),
                    item.getOrderNo(),
                    item.getActionType(),
                    resolveActionLabel(item.getActionType()),
                    OrderViewFormatter.formatStatusDesc(item.getBeforeStatus()),
                    OrderViewFormatter.formatStatusDesc(item.getAfterStatus()),
                    item.getReason(),
                    item.getAdminMemoSnapshot(),
                    item.getDeliveryCompany(),
                    item.getTrackingNum(),
                    item.getActorNo(),
                    resolveActorName(item),
                    OrderViewFormatter.formatDateTime(item.getActionDtm())
            );
        }

        private static String resolveActorName(OrderHistoryListResDto item) {
            if (item.getActorName() != null && !item.getActorName().isBlank()) {
                return item.getActorName();
            }
            return item.getActorNo() == null ? "-" : "관리자#" + item.getActorNo();
        }

        private static String resolveActionLabel(String actionType) {
            return switch (actionType) {
                case "DELIVERY_START" -> "배송 시작";
                case "DELIVERY_COMPLETE" -> "배송 완료";
                case "CANCEL" -> "주문 취소";
                case "ADMIN_MEMO" -> "메모 저장";
                default -> "상태 변경";
            };
        }
    }

    public record AppliedQuery(
            Long orderNo,
            String actionType,
            String keyword,
            String actorKeyword,
            String startDate,
            String endDate,
            String orderType,
            String orderTypeLabel
    ) {
        public static AppliedQuery from(OrderHistoryListQuery query) {
            return new AppliedQuery(
                    query.orderNo(),
                    query.actionType(),
                    query.keyword(),
                    query.actorKeyword(),
                    query.startDate() == null ? null : query.startDate().toString(),
                    query.endDate() == null ? null : query.endDate().toString(),
                    query.orderType() == null ? OrderHistoryOrderType.LATEST.getCode() : query.orderType().getCode(),
                    query.orderType() == null ? OrderHistoryOrderType.LATEST.getDesc() : query.orderType().getDesc()
            );
        }
    }
}
