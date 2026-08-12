package com.section.common.commerce.repository;

import com.section.common.commerce.entity.FrontProductReview;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FrontProductReviewRepository extends JpaRepository<FrontProductReview, Long> {

    Page<FrontProductReview> findByProductNoAndStatusOrderByIdDesc(long productNo, String status, Pageable pageable);

    Page<FrontProductReview> findByMemberNoOrderByIdDesc(long memberNo, Pageable pageable);

    Page<FrontProductReview> findAllByOrderByIdDesc(Pageable pageable);

    Page<FrontProductReview> findByStatusOrderByIdDesc(String status, Pageable pageable);

    @Query("select review from FrontProductReview review where (:status is null or review.status = :status) "
            + "and exists (select 1 from FrontProductReviewReport report where report.reviewNo = review.id) order by review.id desc")
    Page<FrontProductReview> findReportedReviews(
            @Param("status") String status,
            Pageable pageable
    );

    Optional<FrontProductReview> findByIdAndMemberNo(long id, long memberNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select review from FrontProductReview review where review.id = :reviewId")
    Optional<FrontProductReview> findByIdForUpdate(@Param("reviewId") long reviewId);

    boolean existsByMemberNoAndOrderNoAndProductNo(long memberNo, long orderNo, long productNo);

    @Query("select count(review), coalesce(avg(review.rating), 0) from FrontProductReview review "
            + "where review.productNo = :productNo and review.status = :status")
    Object[] getSummaryByProductNo(@Param("productNo") long productNo, @Param("status") String status);
}
