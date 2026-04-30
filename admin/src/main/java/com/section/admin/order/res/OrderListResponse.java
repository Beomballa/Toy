package com.section.admin.order.res;

import com.section.common.base.entity.type.OrderStatus;
import com.section.common.commerce.dto.OrderListItemDto;
import com.section.common.util.DateUtil;
import org.springframework.data.domain.Page;

import java.util.List;

public record OrderListResponse(
        List<OrderItem> orders,
        int currentPage,
        int totalPages,
        long totalElements
) {
    public static OrderListResponse of(Page<OrderListItemDto> page) {
        return new OrderListResponse(
                page.getContent().stream().map(OrderItem::from).toList(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements()
        );
    }

    public record OrderItem(
            Long orderNo,
            String orderNum,
            String buyerName,
            String buyerPhone,
            String productSummary,
            String totalAmount,
            String statusDesc,
            String statusCode,
            String orderDt
    ) {
        public static OrderItem from(OrderListItemDto dto) {
            return new OrderItem(
                    dto.getOrderNo(),
                    dto.getOrderNum(),
                    dto.getBuyerName(),
                    dto.getBuyerPhone(),
                    buildProductSummary(dto.getFirstProductName(), dto.getItemCount()),
                    String.format("%,d원", dto.getTotalAmount()),
                    OrderStatus.resolveDesc(dto.getStatus()),
                    dto.getStatus(),
                    dto.getCrtDtm() != null ? DateUtil.localDateTimeToStr(dto.getCrtDtm()) : ""
            );
        }

        private static String buildProductSummary(String firstProductName, Long itemCount) {
            if (firstProductName == null || firstProductName.isBlank()) {
                return "-";
            }
            if (itemCount == null || itemCount <= 1) {
                return firstProductName;
            }
            return firstProductName + " 외 " + (itemCount - 1) + "건";
        }
    }
}
