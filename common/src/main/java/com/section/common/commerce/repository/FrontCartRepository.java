package com.section.common.commerce.repository;

import com.section.common.commerce.entity.FrontCart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FrontCartRepository extends JpaRepository<FrontCart, Long> {
    Optional<FrontCart> findByCartTokenAndStatus(String cartToken, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fc FROM FrontCart fc WHERE fc.cartToken = :cartToken")
    Optional<FrontCart> findByCartTokenForUpdate(@Param("cartToken") String cartToken);
}
