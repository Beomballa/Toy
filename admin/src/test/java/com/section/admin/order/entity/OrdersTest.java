package com.section.admin.order.entity;

import com.section.common.base.entity.type.OrderStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.entity.Orders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrdersTest {

    @Test
    @DisplayName("배송 시작은 결제 완료 상태에서만 가능하다")
    void startDeliveryThrowsBusinessExceptionWhenStatusInvalid() {
        Orders order = Orders.createOrder("ORD-1", "홍길동", "010-1111-2222", 10000);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> order.startDelivery("CJ대한통운", "123456")
        );

        assertEquals(ErrorCode.ORDER_STATUS_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송 완료는 배송 중 상태에서만 가능하다")
    void completeDeliveryThrowsBusinessExceptionWhenStatusInvalid() {
        Orders order = Orders.createOrder("ORD-2", "홍길동", "010-1111-2222", 10000);
        order.pay();

        BusinessException exception = assertThrows(BusinessException.class, order::completeDelivery);

        assertEquals(ErrorCode.ORDER_STATUS_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    @DisplayName("배송이 시작된 주문은 취소할 수 없다")
    void cancelThrowsBusinessExceptionWhenOrderAlreadyShipped() {
        Orders order = Orders.createOrder("ORD-3", "홍길동", "010-1111-2222", 10000);
        order.pay();
        order.startDelivery("CJ대한통운", "123456");

        BusinessException exception = assertThrows(BusinessException.class, order::cancel);

        assertEquals(ErrorCode.ORDER_STATUS_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    @DisplayName("일반 상태 변경은 허용된 전이만 통과한다")
    void changeStatusAllowsExpectedTransition() {
        Orders order = Orders.createOrder("ORD-4", "홍길동", "010-1111-2222", 10000);
        order.pay();

        order.changeStatus(OrderStatus.PREPARING);

        assertEquals(OrderStatus.PREPARING.name(), order.getStatus());
    }

    @Test
    @DisplayName("일반 상태 변경은 허용되지 않은 전이에서 ORDER_STATUS_NOT_ALLOWED를 던진다")
    void changeStatusThrowsBusinessExceptionWhenTransitionInvalid() {
        Orders order = Orders.createOrder("ORD-5", "홍길동", "010-1111-2222", 10000);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> order.changeStatus(OrderStatus.DELIVERED)
        );

        assertEquals(ErrorCode.ORDER_STATUS_NOT_ALLOWED, exception.getErrorCode());
    }
}
