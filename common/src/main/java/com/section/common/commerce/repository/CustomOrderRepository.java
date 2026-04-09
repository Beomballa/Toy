package com.section.common.commerce.repository;

import com.section.common.commerce.dto.OrderListReqDto;
import com.section.common.commerce.dto.OrderListResDto;
import com.section.common.commerce.dto.OrderItemResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomOrderRepository {
    Page<OrderListResDto> getOrderList(OrderListReqDto reqDto, Pageable pageable);
    
    OrderListResDto getOrderDetail(Long orderNo);
    
    List<OrderItemResDto> getOrderItems(Long orderNo);
}
