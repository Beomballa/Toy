package com.section.admin.order.res;

import com.section.admin.log.res.AdminLogSourceLinkSupport;
import com.section.admin.order.support.OrderViewFormatter;
import com.section.common.base.entity.type.OrderStatus;
import com.section.common.commerce.entity.OrderStatusHistory;
import com.section.common.commerce.dto.OrderListResDto;
import com.section.common.commerce.dto.OrderItemResDto;

import java.util.List;
import java.util.Optional;

public record OrderDetailResponse(
        Long orderNo,
        String orderNum,
        String buyerName,
        String buyerPhone,
        String totalAmount,
        String statusDesc,
        String statusCode,
        String orderDt,
        boolean canCancel,
        boolean canStartDelivery,
        boolean canCompleteDelivery,
        boolean showDeliveryInput,
        boolean showDeliveryInfo,
        String deliveryCompany,
        String trackingNum,
        String adminMemo,
        List<OrderItemInfo> items,
        List<OrderHistoryItem> histories
) {
    public static OrderDetailResponse from(
            OrderListResDto master,
            List<OrderItemResDto> items,
            List<OrderStatusHistory> histories
    ) {
        OrderStatus status = OrderStatus.fromCode(master.getStatus()).orElse(null);

        return new OrderDetailResponse(
                master.getOrderNo(),
                master.getOrderNum(),
                master.getBuyerName(),
                master.getBuyerPhone(),
                OrderViewFormatter.formatAmount(master.getTotalAmount()),
                OrderViewFormatter.formatStatusDesc(master.getStatus()),
                master.getStatus(),
                OrderViewFormatter.formatDateTime(master.getCrtDtm()),
                status != null && status.canCancel(),
                status != null && status.canStartDelivery(),
                status != null && status.canCompleteDelivery(),
                status != null && status.showDeliveryInput(),
                status != null && status.showDeliveryInfo(),
                master.getDeliveryCompany(),
                master.getTrackingNum(),
                master.getAdminMemo(),
                Optional.ofNullable(items)
                        .map(list -> list.stream().map(OrderItemInfo::from).toList())
                        .orElse(List.of()),
                Optional.ofNullable(histories)
                        .map(list -> list.stream().map(OrderHistoryItem::from).toList())
                        .orElse(List.of())
        );
    }

    public record OrderItemInfo(
            Long orderItemNo,
            Long productNo,
            String productName,
            String orderPrice,
            Integer count,
            String thumbnailUrl
    ) {
        public static OrderItemInfo from(OrderItemResDto dto) {
            return new OrderItemInfo(
                    dto.getOrderItemNo(),
                    dto.getProductNo(),
                    dto.getProductName(),
                    OrderViewFormatter.formatAmount(dto.getOrderPrice()),
                    dto.getCount(),
                    dto.getThumbnailUrl()
            );
        }
    }

    public record OrderHistoryItem(
            Long historyNo,
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
            String crtDtm
    ) {
        public static OrderHistoryItem from(OrderStatusHistory history) {
            return new OrderHistoryItem(
                history.getId(),
                AdminLogSourceLinkSupport.resolveOrderHistoryLogPath(history.getOrderNo(), history.getActionType()),
                "활동 로그 보기",
                history.getActionType(),
                OrderViewFormatter.formatActionLabel(history.getActionType()),
                OrderViewFormatter.formatStatusDesc(history.getBeforeStatus()),
                OrderViewFormatter.formatStatusDesc(history.getAfterStatus()),
                history.getReason(),
                history.getAdminMemoSnapshot(),
                history.getDeliveryCompany(),
                    history.getTrackingNum(),
                    OrderViewFormatter.formatDateTime(history.getCrtDtm())
            );
        }
    }
}
