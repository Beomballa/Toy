package com.section.admin.order.res;

import com.section.admin.order.support.OrderViewFormatter;
import com.section.common.base.entity.type.OrderStatus;
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
        List<OrderItemInfo> items
) {
    public static OrderDetailResponse from(OrderListResDto master, List<OrderItemResDto> items) {
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
                Optional.ofNullable(items)
                        .map(list -> list.stream().map(OrderItemInfo::from).toList())
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
}
