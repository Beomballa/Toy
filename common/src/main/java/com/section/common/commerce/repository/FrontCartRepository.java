package com.section.common.commerce.repository;

import com.section.common.commerce.entity.FrontCart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FrontCartRepository extends JpaRepository<FrontCart, Long> {
    Optional<FrontCart> findByCartTokenAndStatus(String cartToken, String status);
}
