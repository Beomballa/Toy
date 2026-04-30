package com.section.admin.order.res;

import com.section.common.base.entity.type.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusActionPolicyTest {

    @Test
    @DisplayName("결제 완료 상태는 취소와 배송 시작이 가능하다")
    void paidStatusAllowsCancelAndStartDelivery() {
        assertTrue(OrderStatus.PAID.canCancel());
        assertTrue(OrderStatus.PAID.canStartDelivery());
        assertFalse(OrderStatus.PAID.canCompleteDelivery());
        assertTrue(OrderStatus.PAID.showDeliveryInput());
        assertFalse(OrderStatus.PAID.showDeliveryInfo());
    }

    @Test
    @DisplayName("배송 중 상태는 배송 완료만 가능하다")
    void shippedStatusAllowsOnlyCompleteDelivery() {
        assertFalse(OrderStatus.SHIPPED.canCancel());
        assertFalse(OrderStatus.SHIPPED.canStartDelivery());
        assertTrue(OrderStatus.SHIPPED.canCompleteDelivery());
        assertFalse(OrderStatus.SHIPPED.showDeliveryInput());
        assertTrue(OrderStatus.SHIPPED.showDeliveryInfo());
    }

    @Test
    @DisplayName("상태 코드는 enum과 한글명으로 안전하게 해석된다")
    void fromCodeAndResolveDescSafelyHandleUnknownCode() {
        assertSame(OrderStatus.PAID, OrderStatus.fromCode("PAID").orElseThrow());
        assertTrue(OrderStatus.fromCode("UNKNOWN").isEmpty());
        assertEquals("결제완료", OrderStatus.resolveDesc("PAID"));
        assertEquals("UNKNOWN", OrderStatus.resolveDesc("UNKNOWN"));
    }
}
