package com.section.common.commerce.repository;

import com.section.common.commerce.entity.ProductChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductChangeHistoryRepository extends JpaRepository<ProductChangeHistory, Long>, CustomProductChangeHistoryRepository {

    List<ProductChangeHistory> findTop20ByProductNoOrderByHistoryNoDesc(Long productNo);
}
