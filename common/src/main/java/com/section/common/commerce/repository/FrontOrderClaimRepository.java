package com.section.common.commerce.repository;
import com.section.common.commerce.entity.FrontOrderClaim;
import com.section.common.commerce.entity.FrontOrderClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
public interface FrontOrderClaimRepository extends JpaRepository<FrontOrderClaim, Long> {
    boolean existsByOrderNoAndStatusIn(long orderNo, Collection<FrontOrderClaimStatus> statuses);
}
