package com.section.common.commerce.repository;

import com.section.common.commerce.dto.OrderListReqDto;
import com.section.common.commerce.dto.OrderListResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomOrderRepository {
    Page<OrderListResDto> getOrderList(OrderListReqDto reqDto, Pageable pageable);
}
