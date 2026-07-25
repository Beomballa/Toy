package com.section.common.commerce.repository;

import com.section.common.commerce.entity.FrontCartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FrontCartItemRepository extends JpaRepository<FrontCartItem, Long> {
    List<FrontCartItem> findAllByCartNoOrderByIdDesc(Long cartNo);
    Optional<FrontCartItem> findByCartNoAndProductNoAndOptionNo(Long cartNo, Long productNo, Long optionNo);
    Optional<FrontCartItem> findByIdAndCartNo(Long id, Long cartNo);
}
