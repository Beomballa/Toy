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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("주문 목록 검색은 공백 단위 다중 키워드와 전화번호 숫자 검색을 함께 지원한다")
    void getOrderListSupportsTokenizedKeywordAndPhoneDigits() {
        Orders matchedOrder = Orders.createOrder("ORD-202605-001", "함장님", "010-1234-5678", 120000);
        matchedOrder.pay();
        orderRepository.save(matchedOrder);
        setOrderCreatedAt(matchedOrder, LocalDateTime.of(2026, 5, 20, 10, 0));
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
        setOrderCreatedAt(otherOrder, LocalDateTime.of(2026, 5, 20, 11, 0));
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
    @DisplayName("주문 목록 검색은 운송장번호 숫자와 관리자 메모 검색을 함께 지원한다")
    void getOrderListSupportsTrackingNumberAndAdminMemoKeyword() {
        Orders matchedOrder = Orders.createOrder("ORD-202606-301", "운송고객", "010-7777-8888", 56000);
        matchedOrder.pay();
        matchedOrder.startDelivery("CJ대한통운", "1234-5678-9000");
        matchedOrder.updateAdminMemo("문 앞에 놓아주세요");
        orderRepository.save(matchedOrder);
        setOrderCreatedAt(matchedOrder, LocalDateTime.now().minusDays(1));
        orderItemRepository.save(OrderItem.builder()
                .orderNo(matchedOrder.getId())
                .productNo(3L)
                .productName("러닝화")
                .orderPrice(56000)
                .count(1)
                .build());

        Orders otherOrder = Orders.createOrder("ORD-202606-302", "일반고객", "010-9999-0000", 42000);
        otherOrder.pay();
        otherOrder.updateAdminMemo("일반 배송");
        orderRepository.save(otherOrder);
        setOrderCreatedAt(otherOrder, LocalDateTime.now().minusDays(1));
        orderItemRepository.save(OrderItem.builder()
                .orderNo(otherOrder.getId())
                .productNo(4L)
                .productName("반팔 티셔츠")
                .orderPrice(42000)
                .count(1)
                .build());

        Page<OrderListItemDto> result = orderRepository.getOrderList(
                new OrderListQuery(
                        null,
                        "123456789000 문 앞",
                        LocalDateTime.now().minusDays(7),
                        LocalDateTime.now()
                ),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("ORD-202606-301", result.getContent().getFirst().getOrderNum());
    }

    @Test
    @DisplayName("최근 7일 매출은 같은 날 주문을 일자 단위로 합산한다")
    void getSalesLast7DaysAggregatesByDate() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        long beforeAmount = extractAmount(orderRepository.getSalesLast7Days(), targetDate);

        Orders morningOrder = Orders.createOrder("ORD-202605-101", "아침고객", "010-1111-2222", 12000);
        morningOrder.pay();
        orderRepository.save(morningOrder);
        setOrderCreatedAt(morningOrder, targetDate.atTime(9, 0));

        Orders eveningOrder = Orders.createOrder("ORD-202605-102", "저녁고객", "010-3333-4444", 18000);
        eveningOrder.pay();
        orderRepository.save(eveningOrder);
        setOrderCreatedAt(eveningOrder, targetDate.atTime(19, 30));

        List<Map<String, Object>> result = orderRepository.getSalesLast7Days();

        Map<String, Object> chartPoint = result.stream()
                .filter(item -> targetDate.toString().equals(item.get("date")))
                .findFirst()
                .orElseThrow();
        assertEquals(beforeAmount + 30000L, ((Number) chartPoint.get("amount")).longValue());
        assertEquals(7, result.size());
    }

    @Test
    @DisplayName("오늘 매출 요약은 결제 이전 주문을 제외하고 결제 이후 상태만 합산한다")
    void getTodaySummaryExcludesUnpaidOrdersFromRevenue() {
        Map<String, Object> beforeSummary = orderRepository.getTodaySummary();

        Orders ordered = Orders.createOrder("ORD-202606-401", "미결제고객", "010-1000-1000", 10000);
        orderRepository.save(ordered);
        setOrderCreatedAt(ordered, LocalDateTime.now().withHour(10).withMinute(0));

        Orders paid = Orders.createOrder("ORD-202606-402", "결제고객", "010-2000-2000", 20000);
        paid.pay();
        orderRepository.save(paid);
        setOrderCreatedAt(paid, LocalDateTime.now().withHour(11).withMinute(0));

        Orders shipped = Orders.createOrder("ORD-202606-403", "배송고객", "010-3000-3000", 30000);
        shipped.pay();
        shipped.startDelivery("CJ대한통운", "777788889999");
        orderRepository.save(shipped);
        setOrderCreatedAt(shipped, LocalDateTime.now().withHour(12).withMinute(0));

        Map<String, Object> summary = orderRepository.getTodaySummary();

        assertEquals(
                ((Number) beforeSummary.get("todayOrderCount")).longValue() + 3L,
                ((Number) summary.get("todayOrderCount")).longValue()
        );
        assertEquals(
                ((Number) beforeSummary.get("todayTotalAmount")).longValue() + 50000L,
                ((Number) summary.get("todayTotalAmount")).longValue()
        );
    }

    @Test
    @DisplayName("브랜드 매출은 주문 총액이 아닌 브랜드별 주문상품 금액 합계로 계산한다")
    void getTopBrandsBySalesUsesOrderItemAmounts() {
        Product brandAProduct = productRepository.save(Product.builder()
                .brandNo(501L)
                .categoryNo(1L)
                .nameKo("브랜드A 상품")
                .releasePrice(10000)
                .status("ACTIVE")
                .build());
        Product brandBProduct = productRepository.save(Product.builder()
                .brandNo(502L)
                .categoryNo(1L)
                .nameKo("브랜드B 상품")
                .releasePrice(7000)
                .status("ACTIVE")
                .build());

        Orders order = Orders.createOrder("ORD-202605-201", "브랜드고객", "010-5555-6666", 17000);
        order.pay();
        orderRepository.save(order);
        setOrderCreatedAt(order, LocalDateTime.of(2026, 5, 21, 10, 0));

        orderItemRepository.save(OrderItem.builder()
                .orderNo(order.getId())
                .productNo(brandAProduct.getId())
                .productName("브랜드A 상품")
                .orderPrice(10000)
                .count(1)
                .build());
        orderItemRepository.save(OrderItem.builder()
                .orderNo(order.getId())
                .productNo(brandBProduct.getId())
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

    @Test
    @DisplayName("인기 상품 집계는 취소 주문을 제외한 판매 수량만 반영한다")
    void getTopSellingProductsExcludesCancelledOrders() {
        String productName = "취소제외검증상품-AX9";
        Orders paidOrder = Orders.createOrder("ORD-202606-501", "정상고객", "010-1111-0000", 10000);
        paidOrder.pay();
        orderRepository.save(paidOrder);
        setOrderCreatedAt(paidOrder, LocalDateTime.now().minusDays(1));
        orderItemRepository.save(OrderItem.builder()
                .orderNo(paidOrder.getId())
                .productNo(10L)
                .productName(productName)
                .orderPrice(5000)
                .count(2)
                .build());

        Orders cancelledOrder = Orders.createOrder("ORD-202606-502", "취소고객", "010-2222-0000", 15000);
        cancelledOrder.pay();
        cancelledOrder.cancel();
        orderRepository.save(cancelledOrder);
        setOrderCreatedAt(cancelledOrder, LocalDateTime.now().minusDays(1));
        orderItemRepository.save(OrderItem.builder()
                .orderNo(cancelledOrder.getId())
                .productNo(10L)
                .productName(productName)
                .orderPrice(5000)
                .count(5)
                .build());

        List<Map<String, Object>> result = orderRepository.getTopSellingProducts(5);

        Map<String, Object> topProduct = result.stream()
                .filter(item -> productName.equals(item.get("name")))
                .findFirst()
                .orElseThrow();
        assertEquals(2L, ((Number) topProduct.get("count")).longValue());
    }

    private long extractAmount(List<Map<String, Object>> salesChart, LocalDate targetDate) {
        return salesChart.stream()
                .filter(item -> targetDate.toString().equals(item.get("date")))
                .findFirst()
                .map(item -> ((Number) item.get("amount")).longValue())
                .orElse(0L);
    }

    private void setOrderCreatedAt(Orders order, LocalDateTime createdAt) {
        orderRepository.flush();
        jdbcTemplate.update(
                "UPDATE orders SET crt_dtm = ? WHERE order_no = ?",
                createdAt,
                order.getId()
        );
    }
}
