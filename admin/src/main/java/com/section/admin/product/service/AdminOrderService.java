package com.section.admin.product.service;

import com.section.admin.product.res.OrderDetailResponse;
import com.section.admin.product.res.OrderListResponse;
import com.section.common.base.entity.type.OrderStatus;
import com.section.common.commerce.dto.OrderListReqDto;
import com.section.common.commerce.dto.OrderListResDto;
import com.section.common.commerce.dto.OrderItemResDto;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.entity.OrderItem;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.commerce.service.OrderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    /**
     * 테스트용 더미 데이터 초기화 (주문 상세 포함)
     */
    @PostConstruct
    @Transactional
    public void initDummyData() {
        if (orderRepository.count() == 0) {
            // 1. 주문 생성
            Orders o1 = Orders.createOrder("ORD-20260401-001", "김철수", "010-1234-5678", 139000);
            Orders o2 = Orders.createOrder("ORD-20260401-002", "이영희", "010-9876-5432", 129000);
            Orders o3 = Orders.createOrder("ORD-20260401-003", "박지민", "010-5555-4444", 185000);
            
            orderRepository.saveAll(List.of(o1, o2, o3));
            o1.pay();
            o3.cancel();

            // 2. 주문 상세 더미 (실제 상품이 있을 경우 연결)
            List<Product> products = productRepository.findAll();
            if (!products.isEmpty()) {
                Product p = products.get(0);
                orderItemRepository.save(OrderItem.builder()
                        .orderNo(o1.getId())
                        .productNo(p.getId())
                        .productName(p.getNameKo())
                        .orderPrice(p.getReleasePrice())
                        .count(1)
                        .build());
            }
        }
    }

    /**
     * 화면용 주문 목록 조회
     */
    public OrderListResponse getOrderList(OrderListReqDto reqDto, Pageable pageable) {
        Page<OrderListResDto> result = orderService.getOrderList(reqDto, pageable);
        return OrderListResponse.of(result);
    }

    /**
     * 화면용 주문 상세 조회
     */
    public OrderDetailResponse getOrderDetail(Long orderNo) {
        OrderListResDto master = orderService.getOrderDetail(orderNo);
        if (master == null) throw new IllegalArgumentException("존재하지 않는 주문입니다.");
        
        List<OrderItemResDto> items = orderService.getOrderItems(orderNo);
        return OrderDetailResponse.from(master, items);
    }

    /**
     * 주문 상태 변경
     */
    @Transactional
    public void updateOrderStatus(Long orderNo, String status) {
        orderService.updateOrderStatus(orderNo, OrderStatus.valueOf(status));
    }
}
