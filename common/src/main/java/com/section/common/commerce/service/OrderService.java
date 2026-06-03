package com.section.common.commerce.service;

import com.section.common.base.entity.type.OrderStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.OrderListItemDto;
import com.section.common.commerce.dto.OrderListQuery;
import com.section.common.commerce.dto.OrderListReqDto;
import com.section.common.commerce.dto.OrderListResDto;
import com.section.common.commerce.dto.OrderItemResDto;
import com.section.common.commerce.dto.OrderStatusSummaryDto;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;

    /**
     * 주문 목록 조회
     */
    public Page<OrderListItemDto> getOrderList(OrderListReqDto reqDto, Pageable pageable) {
        return orderRepository.getOrderList(reqDto.toQuery(), pageable);
    }

    public List<OrderStatusSummaryDto> getOrderStatusSummaries(OrderListReqDto reqDto) {
        return orderRepository.getOrderStatusSummaries(reqDto.toQuery());
    }

    /**
     * 주문 상세 조회 (마스터)
     */
    public OrderListResDto getOrderDetail(Long orderNo) {
        return orderRepository.getOrderDetail(orderNo);
    }

    /**
     * 주문 상품 목록 조회
     */
    public List<OrderItemResDto> getOrderItems(Long orderNo) {
        return orderRepository.getOrderItems(orderNo);
    }

    /**
     * 주문 상태 변경
     */
    @Transactional
    public void updateOrderStatus(Long orderNo, OrderStatus status) {
        Orders order = orderRepository.findById(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.changeStatus(status);
    }
}
