package com.section.admin.order.service;

import com.section.admin.order.support.OrderExportCsvWriter;
import com.section.common.base.entity.type.OrderStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.OrderHistoryListResDto;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.entity.OrderStatusHistory;
import com.section.common.commerce.repository.*;
import com.section.common.commerce.service.OrderService;
import com.section.admin.order.req.OrderHistoryListRequest;
import com.section.admin.order.res.OrderHistoryListResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    @Test
    @DisplayName("주문 처리 이력 목록은 작업자명과 메타 정보를 함께 반환한다")
    void getOrderHistoryListReturnsPagedHistory() {
        OrderHistoryListRequest request = new OrderHistoryListRequest();
        request.setOrderNo(3L);
        request.setActionType("DELIVERY_START");
        request.setActorKeyword("관리자");
        request.setOrderType("oldest");

        OrderHistoryListResDto row = new OrderHistoryListResDto();
        row.setHistoryNo(7L);
        row.setOrderNo(3L);
        row.setActionType("DELIVERY_START");
        row.setBeforeStatus("PREPARING");
        row.setAfterStatus("SHIPPED");
        row.setReason("출고 완료");
        row.setDeliveryCompany("CJ대한통운");
        row.setTrackingNum("1234");
        row.setActorNo(1L);
        row.setActorName("관리자");
        row.setActionDtm(java.time.LocalDateTime.of(2026, 5, 16, 10, 0));

        when(orderStatusHistoryRepository.getOrderHistoryList(any(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

        OrderHistoryListResponse response = adminOrderService.getOrderHistoryList(request, PageRequest.of(0, 20));

        assertEquals(1, response.items().size());
        assertEquals("관리자", response.items().get(0).actorName());
        assertEquals("배송 시작", response.items().get(0).actionLabel());
        assertEquals("DELIVERY_START", response.appliedQuery().actionType());
        assertEquals("관리자", response.appliedQuery().actorKeyword());
        assertEquals("oldest", response.appliedQuery().orderType());
        assertEquals("1-1 / 1건 · 1페이지", response.pageInfoLabel());
    }
}
