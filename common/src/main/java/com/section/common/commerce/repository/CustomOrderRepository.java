package com.section.common.commerce.repository;

import com.section.common.commerce.dto.OrderItemResDto;
import com.section.common.commerce.dto.OrderListReqDto;
import com.section.common.commerce.dto.OrderListResDto;
import com.section.common.commerce.dto.OrderListResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface CustomOrderRepository {
    Page<OrderListResDto> getOrderList(OrderListReqDto reqDto, Pageable pageable);

    OrderListResDto getOrderDetail(Long orderNo);

    List<OrderItemResDto> getOrderItems(Long orderNo);

    // 대시보드용
    Map<String, Object> getTodaySummary();
    List<OrderListResDto> getRecentOrders(int limit);
    List<Map<String, Object>> getSalesLast7Days();
    List<Map<String, Object>> getTopSellingProducts(int limit);
    List<Map<String, Object>> getTopBrandsBySales(int limit);
}

