package com.section.common.commerce.repository;

import com.section.common.commerce.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long>, CustomOrderRepository {
    Optional<Orders> findByOrderNum(String orderNum);
}
