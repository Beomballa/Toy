package com.section.common.commerce.repository;

import com.section.common.commerce.entity.FrontProductDisplay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface FrontProductDisplayRepository extends JpaRepository<FrontProductDisplay, Long> {

    Optional<FrontProductDisplay> findByProductNo(Long productNo);

    @Query("""
            SELECT COUNT(display) > 0
            FROM FrontProductDisplay display
            JOIN Product product ON product.id = display.productNo
            WHERE display.featuredYn = :featuredYn
              AND display.featuredRank = :featuredRank
              AND display.productNo <> :productNo
              AND product.status = :productStatus
            """)
    boolean existsFeaturedRankConflict(
            @Param("featuredYn") String featuredYn,
            @Param("featuredRank") Integer featuredRank,
            @Param("productNo") Long productNo,
            @Param("productStatus") String productStatus
    );

    @Query("""
            SELECT display.featuredRank
            FROM FrontProductDisplay display
            JOIN Product product ON product.id = display.productNo
            WHERE display.featuredYn = 'Y'
              AND product.status = :productStatus
              AND (:excludedProductNo IS NULL OR display.productNo <> :excludedProductNo)
            ORDER BY display.featuredRank ASC
            """)
    List<Integer> findActiveFeaturedRanks(
            @Param("productStatus") String productStatus,
            @Param("excludedProductNo") Long excludedProductNo
    );
}
