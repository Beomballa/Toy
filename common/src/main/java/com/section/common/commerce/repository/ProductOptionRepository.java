package com.section.common.commerce.repository;

import com.section.common.commerce.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository extends JpaRepository<ProductOption,Long> {

    @Query("SELECT po FROM ProductOption po WHERE po.productNo =:productId")
    List<ProductOption> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM ProductOption po WHERE po.productNo = :productId ORDER BY po.id ASC")
    List<ProductOption> findByProductIdForUpdate(@Param("productId") Long productId);

    List<ProductOption> findAllByProductNoInOrderByProductNoAscOptionNameAsc(List<Long> productNos);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM ProductOption po WHERE po.id = :optionId")
    Optional<ProductOption> findByIdForUpdate(Long optionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM ProductOption po WHERE po.id IN :optionIds ORDER BY po.id ASC")
    List<ProductOption> findAllByIdForUpdate(@Param("optionIds") List<Long> optionIds);

    void deleteByProductNo(Long productNo);
}
