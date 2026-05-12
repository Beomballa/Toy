package com.section.admin.order.service;

import com.section.admin.order.support.OrderExportCsvWriter;
import com.section.common.base.entity.type.OrderStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.entity.OrderStatusHistory;
import com.section.common.commerce.repository.*;
import com.section.common.commerce.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductOptionRepository productOptionRepository;
    @Mock private OrderService orderService;

    @InjectMocks
    private AdminOrderService adminOrderService;

    @Test
    @DisplayName("일반 주문 상태 변경은 상태 이력을 남긴다")
    void updateOrderStatusCreatesHistory() {
        Orders order = Orders.createOrder("ORD-1", "홍길동", "010", 1000);
        order.pay();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        adminOrderService.updateOrderStatus(1L, OrderStatus.PREPARING, "출고 준비");

        assertEquals(OrderStatus.PREPARING.name(), order.getStatus());
        ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(orderStatusHistoryRepository).save(historyCaptor.capture());
        assertEquals("STATUS_CHANGE", historyCaptor.getValue().getActionType());
        assertEquals("PAID", historyCaptor.getValue().getBeforeStatus());
        assertEquals("PREPARING", historyCaptor.getValue().getAfterStatus());
        assertEquals("출고 준비", historyCaptor.getValue().getReason());
    }

    @Test
    @DisplayName("관리 메모 저장은 현재 상태를 유지한 채 이력을 남긴다")
    void saveAdminMemoCreatesHistoryWithSnapshot() {
        Orders order = Orders.createOrder("ORD-2", "홍길동", "010", 1000);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));

        adminOrderService.saveAdminMemo(2L, "고객 요청 확인");

        assertEquals("고객 요청 확인", order.getAdminMemo());
        ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(orderStatusHistoryRepository).save(historyCaptor.capture());
        assertEquals("ADMIN_MEMO", historyCaptor.getValue().getActionType());
        assertEquals(order.getStatus(), historyCaptor.getValue().getBeforeStatus());
        assertEquals(order.getStatus(), historyCaptor.getValue().getAfterStatus());
        assertEquals("고객 요청 확인", historyCaptor.getValue().getAdminMemoSnapshot());
    }

    @Test
    @DisplayName("존재하지 않는 주문의 메모 저장은 ORDER_NOT_FOUND를 던진다")
    void saveAdminMemoThrowsWhenOrderMissing() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminOrderService.saveAdminMemo(999L, "메모")
        );

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
        verify(orderStatusHistoryRepository, never()).save(any());
    }
}
