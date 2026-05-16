package com.section.common.commerce.repository;

import com.section.common.commerce.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long>, CustomOrderStatusHistoryRepository {
    List<OrderStatusHistory> findTop20ByOrderNoOrderByCrtDtmDescIdDesc(Long orderNo);
}
