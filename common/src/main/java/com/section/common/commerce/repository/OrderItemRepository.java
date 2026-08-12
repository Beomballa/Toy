package com.section.common.commerce.repository;

import com.section.common.commerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderNo(Long orderNo);
    List<OrderItem> findAllByOrderNoInOrderByOrderNoAscIdAsc(List<Long> orderNos);
    boolean existsByOrderNoAndProductNo(Long orderNo, Long productNo);
    boolean existsByOptionNo(Long optionNo);
}
