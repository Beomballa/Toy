package com.section.admin.order.res;

import com.section.admin.log.res.AdminLogSourceLinkSupport;
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
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static OrderHistoryListResponse of(Page<OrderHistoryListResDto> page, OrderHistoryListQuery query) {
        long rangeStart = page.getTotalElements() == 0 ? 0 : page.getNumber() * page.getSize() + 1L;
        long rangeEnd = page.getTotalElements() == 0 ? 0 : Math.min(page.getTotalElements(), rangeStart + page.getNumberOfElements() - 1L);
        String pageInfoLabel = page.getTotalElements() == 0
                ? "조회 결과 없음"
                : "%d-%d / %d건 · %d페이지".formatted(rangeStart, rangeEnd, page.getTotalElements(), Math.max(page.getTotalPages(), 1));

        return new OrderHistoryListResponse(
                page.getContent().stream().map(Item::from).toList(),
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
            Long orderNo,
            String activityLogPath,
            String activityLogLabel,
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
                    AdminLogSourceLinkSupport.resolveOrderHistoryLogPath(item.getOrderNo(), item.getActionType()),
                    "활동 로그 보기",
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
            Long actorNo,
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
                    query.actorNo(),
                    query.actorKeyword(),
                    query.startDate() == null ? null : query.startDate().toString(),
                    query.endDate() == null ? null : query.endDate().toString(),
                    query.orderType() == null ? OrderHistoryOrderType.LATEST.getCode() : query.orderType().getCode(),
                    query.orderType() == null ? OrderHistoryOrderType.LATEST.getDesc() : query.orderType().getDesc()
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
                Page<OrderHistoryListResDto> page,
                OrderHistoryListQuery query,
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

        private static int countFilters(OrderHistoryListQuery query) {
            int count = 0;
            if (query.orderNo() != null) count += 1;
            if (query.actionType() != null && !query.actionType().isBlank()) count += 1;
            if (query.keyword() != null && !query.keyword().isBlank()) count += 1;
            if (query.actorNo() != null) count += 1;
            if (query.actorKeyword() != null && !query.actorKeyword().isBlank()) count += 1;
            if (query.startDate() != null) count += 1;
            if (query.endDate() != null) count += 1;
            if (query.orderType() != null && query.orderType() != OrderHistoryOrderType.LATEST) count += 1;
            return count;
        }

        private static String buildQuerySignature(OrderHistoryListQuery query, long rangeStart, long rangeEnd) {
            StringBuilder builder = new StringBuilder();
            builder.append(rangeStart).append("-").append(rangeEnd);
            if (query.orderNo() != null) builder.append(" · 주문=").append(query.orderNo());
            if (query.actionType() != null && !query.actionType().isBlank()) builder.append(" · 작업=").append(query.actionType());
            if (query.keyword() != null && !query.keyword().isBlank()) builder.append(" · 검색=").append(query.keyword());
            if (query.actorNo() != null) builder.append(" · 작업자번호=").append(query.actorNo());
            if (query.actorKeyword() != null && !query.actorKeyword().isBlank()) builder.append(" · 작업자=").append(query.actorKeyword());
            if (query.orderType() != null && query.orderType() != OrderHistoryOrderType.LATEST) builder.append(" · 정렬=").append(query.orderType().getDesc());
            return builder.toString();
        }
    }
}
