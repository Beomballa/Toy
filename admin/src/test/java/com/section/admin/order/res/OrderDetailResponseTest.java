package com.section.admin.order.res;

import com.section.common.commerce.dto.OrderListResDto;
import com.section.common.commerce.entity.OrderStatusHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderDetailResponseTest {

    @Test
    @DisplayName("결제 완료 주문은 취소와 배송 시작이 가능하다")
    void fromReturnsAvailableActionsForPaidStatus() {
        OrderListResDto master = OrderListResDto.builder()
                .orderNo(1L)
                .orderNum("ORD-1")
                .buyerName("홍길동")
                .buyerPhone("010-1111-2222")
                .totalAmount(10000)
                .status("PAID")
                .adminMemo("메모")
                .build();

        OrderDetailResponse response = OrderDetailResponse.from(master, List.of(), List.of());

        assertTrue(response.canCancel());
        assertTrue(response.canStartDelivery());
        assertFalse(response.canCompleteDelivery());
        assertTrue(response.showDeliveryInput());
        assertFalse(response.showDeliveryInfo());
        assertEquals("메모", response.adminMemo());
    }

    @Test
    @DisplayName("배송 중 주문은 배송 완료만 가능하다")
    void fromReturnsAvailableActionsForShippedStatus() {
        OrderListResDto master = OrderListResDto.builder()
                .orderNo(2L)
                .orderNum("ORD-2")
                .buyerName("홍길동")
                .buyerPhone("010-1111-2222")
                .totalAmount(10000)
                .status("SHIPPED")
                .deliveryCompany("CJ대한통운")
                .trackingNum("1234")
                .build();

        OrderStatusHistory history = OrderStatusHistory.create(2L, "DELIVERY_START", "PAID", "SHIPPED", "출고", null, "CJ대한통운", "1234");
        OrderDetailResponse response = OrderDetailResponse.from(master, List.of(), List.of(history));

        assertFalse(response.canCancel());
        assertFalse(response.canStartDelivery());
        assertTrue(response.canCompleteDelivery());
        assertFalse(response.showDeliveryInput());
        assertTrue(response.showDeliveryInfo());
        assertEquals("CJ대한통운", response.deliveryCompany());
        assertEquals(1, response.histories().size());
        assertEquals("/admin/logs?actionType=ORDER_DELIVERY_START&targetId=2", response.histories().get(0).activityLogPath());
    }
}
