package com.section.common.commerce.repository;
import com.section.common.commerce.entity.FrontOrderClaim;
import com.section.common.commerce.entity.FrontOrderClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
public interface FrontOrderClaimRepository extends JpaRepository<FrontOrderClaim, Long> {
    boolean existsByOrderNoAndStatusIn(long orderNo, Collection<FrontOrderClaimStatus> statuses);

    Page<FrontOrderClaim> findByMemberNoOrderByCrtDtmDescIdDesc(long memberNo, Pageable pageable);

    Page<FrontOrderClaim> findAllByOrderByIdDesc(Pageable pageable);

    Page<FrontOrderClaim> findByStatusOrderByIdDesc(FrontOrderClaimStatus status, Pageable pageable);

    java.util.Optional<FrontOrderClaim> findByIdAndMemberNo(long claimNo, long memberNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select claim from FrontOrderClaim claim where claim.id = :claimNo and claim.memberNo = :memberNo")
    java.util.Optional<FrontOrderClaim> findByIdAndMemberNoForUpdate(
            @Param("claimNo") long claimNo,
            @Param("memberNo") long memberNo
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select claim from FrontOrderClaim claim where claim.id = :claimNo")
    java.util.Optional<FrontOrderClaim> findByIdForUpdate(@Param("claimNo") long claimNo);
}
