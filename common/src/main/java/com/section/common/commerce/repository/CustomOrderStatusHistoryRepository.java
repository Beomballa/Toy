package com.section.common.commerce.repository;

import com.section.common.commerce.dto.OrderHistoryListQuery;
import com.section.common.commerce.dto.OrderHistoryListResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomOrderStatusHistoryRepository {
    Page<OrderHistoryListResDto> getOrderHistoryList(OrderHistoryListQuery query, Pageable pageable);
}
