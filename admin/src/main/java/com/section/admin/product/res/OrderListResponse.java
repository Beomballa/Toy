package com.section.admin.product.res;

import com.section.common.base.entity.type.OrderStatus;
import com.section.common.commerce.dto.OrderListResDto;
import com.section.common.util.DateUtil;
import org.springframework.data.domain.Page;

import java.util.List;

public record OrderListResponse(
        List<OrderItem> orders,
        int currentPage,
        int totalPages,
        long totalElements
) {
    public static OrderListResponse of(Page<OrderListResDto> page) {
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
            String totalAmount,
            String statusDesc,
            String statusCode,
            String orderDt
    ) {
        public static OrderItem from(OrderListResDto dto) {
            String statusDesc = dto.getStatus();
            try {
                // String 상태값을 기반으로 Enum의 desc(한글명) 추출
                statusDesc = OrderStatus.valueOf(dto.getStatus()).getDesc();
            } catch (Exception ignored) {
                // 매칭되는 Enum이 없을 경우 원본 문자열 유지
            }

            return new OrderItem(
                    dto.getOrderNo(),
                    dto.getOrderNum(),
                    dto.getBuyerName(),
                    dto.getBuyerPhone(),
                    String.format("%,d원", dto.getTotalAmount()),
                    statusDesc,
                    dto.getStatus(),
                    dto.getCrtDtm() != null ? DateUtil.localDateTimeToStr(dto.getCrtDtm()) : ""
            );
        }
    }
}
