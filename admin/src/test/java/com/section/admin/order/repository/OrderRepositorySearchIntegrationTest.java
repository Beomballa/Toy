package com.section.admin.order.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.base.entity.type.OrderStatus;
import com.section.common.commerce.dto.OrderListItemDto;
import com.section.common.commerce.dto.OrderListQuery;
import com.section.common.commerce.entity.OrderItem;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class OrderRepositorySearchIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

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

    @Test
    @DisplayName("최근 7일 매출은 같은 날 주문을 일자 단위로 합산한다")
    void getSalesLast7DaysAggregatesByDate() {
        Orders morningOrder = Orders.createOrder("ORD-202605-101", "아침고객", "010-1111-2222", 12000);
        morningOrder.pay();
        orderRepository.save(morningOrder);
        morningOrder.setCrtDtm(LocalDateTime.of(2026, 5, 20, 9, 0));

        Orders eveningOrder = Orders.createOrder("ORD-202605-102", "저녁고객", "010-3333-4444", 18000);
        eveningOrder.pay();
        orderRepository.save(eveningOrder);
        eveningOrder.setCrtDtm(LocalDateTime.of(2026, 5, 20, 19, 30));

        List<Map<String, Object>> result = orderRepository.getSalesLast7Days();

        Map<String, Object> chartPoint = result.stream()
                .filter(item -> "2026-05-20".equals(item.get("date")))
                .findFirst()
                .orElseThrow();
        assertEquals(30000L, ((Number) chartPoint.get("amount")).longValue());
    }

    @Test
    @DisplayName("브랜드 매출은 주문 총액이 아닌 브랜드별 주문상품 금액 합계로 계산한다")
    void getTopBrandsBySalesUsesOrderItemAmounts() {
        productRepository.save(Product.builder()
                .id(1001L)
                .brandNo(501L)
                .categoryNo(1L)
                .nameKo("브랜드A 상품")
                .releasePrice(10000)
                .status("ACTIVE")
                .build());
        productRepository.save(Product.builder()
                .id(1002L)
                .brandNo(502L)
                .categoryNo(1L)
                .nameKo("브랜드B 상품")
                .releasePrice(7000)
                .status("ACTIVE")
                .build());

        Orders order = Orders.createOrder("ORD-202605-201", "브랜드고객", "010-5555-6666", 17000);
        order.pay();
        orderRepository.save(order);
        order.setCrtDtm(LocalDateTime.of(2026, 5, 21, 10, 0));

        orderItemRepository.save(OrderItem.builder()
                .orderNo(order.getId())
                .productNo(1001L)
                .productName("브랜드A 상품")
                .orderPrice(10000)
                .count(1)
                .build());
        orderItemRepository.save(OrderItem.builder()
                .orderNo(order.getId())
                .productNo(1002L)
                .productName("브랜드B 상품")
                .orderPrice(3500)
                .count(2)
                .build());

        List<Map<String, Object>> result = orderRepository.getTopBrandsBySales(10);

        Map<String, Object> brandA = result.stream()
                .filter(item -> Long.valueOf(501L).equals(item.get("brandNo")))
                .findFirst()
                .orElseThrow();
        Map<String, Object> brandB = result.stream()
                .filter(item -> Long.valueOf(502L).equals(item.get("brandNo")))
                .findFirst()
                .orElseThrow();
        assertEquals(10000L, ((Number) brandA.get("amount")).longValue());
        assertEquals(7000L, ((Number) brandB.get("amount")).longValue());
    }
}
