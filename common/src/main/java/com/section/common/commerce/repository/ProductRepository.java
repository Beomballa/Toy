package com.section.common.commerce.repository;


import com.section.common.commerce.dto.ProductDetailResDto;
import com.section.common.commerce.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, CustomProductRepository {

    boolean existsByBrandNo(Long brandNo);

    boolean existsByCategoryNo(Long categoryNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT product FROM Product product WHERE product.id = :productNo")
    Optional<Product> findByIdForUpdate(@Param("productNo") Long productNo);

    @Query("SELECT new com.section.common.commerce.dto.ProductDetailResDto(" +
            "p.id, c.categoryNo, c.name, b.brandNo, b.nameKo, " +
            "p.nameKo, p.modelNum, p.releasePrice, p.releaseDt, p.thumbnailUrl, p.status, p.crtDtm, p.uptDtm) " +
            "FROM Product p " +
            "LEFT JOIN Category c ON p.categoryNo = c.categoryNo " +
            "LEFT JOIN Brand b ON p.brandNo = b.brandNo " +
            "WHERE p.id = :productNo")
    ProductDetailResDto findProductDetail(@Param("productNo") Long productNo);
}
