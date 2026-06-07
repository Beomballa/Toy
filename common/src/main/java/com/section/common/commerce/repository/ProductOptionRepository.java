package com.section.common.commerce.repository;

import com.section.common.commerce.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductOptionRepository extends JpaRepository<ProductOption,Long> {

    @Query("SELECT po FROM ProductOption po WHERE po.productNo =:productId")
    List<ProductOption> findByProductId(Long productId);

    List<ProductOption> findAllByProductNoInOrderByProductNoAscOptionNameAsc(List<Long> productNos);

    void deleteByProductNo(Long productNo);
}
