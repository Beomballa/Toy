package com.section.admin.order.res;

import com.section.admin.order.support.OrderViewFormatter;
import com.section.common.commerce.dto.OrderListItemDto;
import com.section.common.commerce.dto.OrderStatusSummaryDto;
import org.springframework.data.domain.Page;

import java.util.List;

public record OrderListResponse(
        List<OrderItem> orders,
        List<StatusSummary> statusSummaries,
        int currentPage,
        int totalPages,
        long totalElements
) {
    public static OrderListResponse of(Page<OrderListItemDto> page, List<OrderStatusSummaryDto> summaries) {
        return new OrderListResponse(
                page.getContent().stream().map(OrderItem::from).toList(),
                summaries == null ? List.of() : summaries.stream().map(StatusSummary::from).toList(),
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
                    OrderViewFormatter.buildProductSummary(dto.getFirstProductName(), dto.getItemCount()),
                    OrderViewFormatter.formatAmount(dto.getTotalAmount()),
                    OrderViewFormatter.formatStatusDesc(dto.getStatus()),
                    dto.getStatus(),
                    OrderViewFormatter.formatDateTime(dto.getCrtDtm())
            );
        }
    }

    public record StatusSummary(
            String statusCode,
            String statusDesc,
            long count
    ) {
        public static StatusSummary from(OrderStatusSummaryDto dto) {
            return new StatusSummary(
                    dto.getStatus(),
                    OrderViewFormatter.formatStatusDesc(dto.getStatus()),
                    dto.getCount() == null ? 0L : dto.getCount()
            );
        }
    }
}
