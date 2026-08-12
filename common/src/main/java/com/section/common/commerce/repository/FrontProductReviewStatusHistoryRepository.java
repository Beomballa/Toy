package com.section.common.commerce.repository;

import com.section.common.commerce.entity.FrontProductReviewStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FrontProductReviewStatusHistoryRepository extends JpaRepository<FrontProductReviewStatusHistory, Long> {

    List<FrontProductReviewStatusHistory> findAllByReviewNoInOrderByIdDesc(List<Long> reviewNos);
}
