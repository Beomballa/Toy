package com.section.admin.order.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.base.entity.type.OrderStatus;
import com.section.common.commerce.dto.OrderListItemDto;
import com.section.common.commerce.dto.OrderListQuery;
import com.section.common.commerce.entity.OrderItem;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class OrderRepositorySearchIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    @DisplayName("주문 목록 검색은 공백 단위 다중 키워드와 전화번호 숫자 검색을 함께 지원한다")
    void getOrderListSupportsTokenizedKeywordAndPhoneDigits() {
        Orders matchedOrder = Orders.createOrder("ORD-202605-001", "함장님", "010-1234-5678", 120000);
        matchedOrder.pay();
        orderRepository.save(matchedOrder);
        matchedOrder.setCrtDtm(LocalDateTime.of(2026, 5, 20, 10, 0));
        orderItemRepository.save(OrderItem.builder()
                .orderNo(matchedOrder.getId())
                .productNo(1L)
                .productName("삼바 인형")
                .orderPrice(120000)
                .count(1)
                .build());

        Orders otherOrder = Orders.createOrder("ORD-202605-002", "다른고객", "010-9999-8888", 90000);
        otherOrder.pay();
        orderRepository.save(otherOrder);
        otherOrder.setCrtDtm(LocalDateTime.of(2026, 5, 20, 11, 0));
        orderItemRepository.save(OrderItem.builder()
                .orderNo(otherOrder.getId())
                .productNo(2L)
                .productName("다른 상품")
                .orderPrice(90000)
                .count(1)
                .build());

        Page<OrderListItemDto> result = orderRepository.getOrderList(
                new OrderListQuery(
                        OrderStatus.PAID,
                        "함장님 0101234 삼바",
                        LocalDateTime.of(2026, 5, 1, 0, 0),
                        LocalDateTime.of(2026, 5, 31, 23, 59, 59)
                ),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("함장님", result.getContent().getFirst().getBuyerName());
        assertEquals("삼바 인형", result.getContent().getFirst().getFirstProductName());
    }
}
