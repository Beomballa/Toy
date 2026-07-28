package com.section.common.commerce.repository;

import com.section.common.commerce.entity.Orders;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long>, CustomOrderRepository {
    Optional<Orders> findByOrderNum(String orderNum);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Orders o WHERE o.id = :orderNo")
    Optional<Orders> findByIdForUpdate(@Param("orderNo") Long orderNo);
}
