package com.section.admin.order.req;

import com.section.common.base.entity.type.OrderStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusUpdateRequestTest {

    @Test
    @DisplayName("유효한 주문 상태 문자열은 enum으로 변환된다")
    void toOrderStatusReturnsEnum() {
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(1L, "SHIPPED", "  출고 승인  ");

        OrderStatus result = request.toOrderStatus();

        assertEquals(OrderStatus.SHIPPED, result);
        assertEquals("출고 승인", request.normalizedReason());
    }

    @Test
    @DisplayName("잘못된 주문 상태 문자열은 INVALID_INPUT_VALUE 예외를 던진다")
    void toOrderStatusThrowsBusinessExceptionWhenInvalid() {
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(1L, "UNKNOWN", null);

        BusinessException exception = assertThrows(BusinessException.class, request::toOrderStatus);

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }
}
