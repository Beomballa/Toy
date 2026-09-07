package com.section.common.commerce.repository;

import com.section.common.commerce.entity.FrontOrderClaimHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FrontOrderClaimHistoryRepository extends JpaRepository<FrontOrderClaimHistory, Long> {
    java.util.List<FrontOrderClaimHistory> findByClaimNoOrderByIdAsc(long claimNo);
}
