package com.section.common.commerce.repository;

import com.section.common.commerce.entity.OrderDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, Long> {
    Optional<OrderDelivery> findByOrderNo(Long orderNo);
}
