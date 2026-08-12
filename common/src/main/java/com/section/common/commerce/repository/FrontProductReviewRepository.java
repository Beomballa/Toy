package com.section.common.commerce.repository;

import com.section.common.commerce.entity.FrontProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FrontProductReviewRepository extends JpaRepository<FrontProductReview, Long> {

    Page<FrontProductReview> findByProductNoOrderByIdDesc(long productNo, Pageable pageable);

    Page<FrontProductReview> findByMemberNoOrderByIdDesc(long memberNo, Pageable pageable);

    boolean existsByMemberNoAndOrderNoAndProductNo(long memberNo, long orderNo, long productNo);

    @Query("select count(review), coalesce(avg(review.rating), 0) from FrontProductReview review where review.productNo = :productNo")
    Object[] getSummaryByProductNo(@Param("productNo") long productNo);
}
