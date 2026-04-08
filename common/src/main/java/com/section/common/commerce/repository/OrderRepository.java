package com.section.common.commerce.repository;

import com.section.common.commerce.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Orders, Long>, CustomOrderRepository {
}
