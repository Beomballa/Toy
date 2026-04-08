package com.section.admin.product.service;

import com.section.admin.product.res.OrderListResponse;
import com.section.common.base.entity.type.OrderStatus;
import com.section.common.commerce.dto.OrderListReqDto;
import com.section.common.commerce.dto.OrderListResDto;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.repository.OrderRepository;
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
    private final OrderService orderService;

    /**
     * 테스트용 더미 데이터 초기화
     */
    @PostConstruct
    @Transactional
    public void initDummyData() {
        if (orderRepository.count() == 0) {
            orderRepository.saveAll(List.of(
                Orders.createOrder("ORD-20260401-001", "김철수", "010-1234-5678", 139000),
                Orders.createOrder("ORD-20260401-002", "이영희", "010-9876-5432", 129000),
                Orders.createOrder("ORD-20260401-003", "박지민", "010-5555-4444", 185000)
            ));
            
            // 특정 상태 업데이트 테스트
            orderRepository.findAll().get(0).pay();
            orderRepository.findAll().get(2).cancel();
        }
    }

    /**
     * 화면용 주문 목록 조회
     */
    public OrderListResponse getOrderList(OrderListReqDto reqDto, Pageable pageable) {
        Page<OrderListResDto> result = orderService.getOrderList(reqDto, pageable);
        return OrderListResponse.of(result);
    }
}
