package com.section.common.commerce.repository;

import com.section.common.commerce.entity.FrontProductReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FrontProductReviewReportRepository extends JpaRepository<FrontProductReviewReport, Long> {

    boolean existsByReviewNoAndMemberNo(long reviewNo, long memberNo);

    List<FrontProductReviewReport> findAllByReviewNoInOrderByIdDesc(List<Long> reviewNos);

    @Query("select report.reviewNo as reviewNo, count(report) as count from FrontProductReviewReport report "
            + "where report.reviewNo in :reviewNos group by report.reviewNo")
    List<ReviewReportCount> countByReviewNoIn(@Param("reviewNos") List<Long> reviewNos);

    interface ReviewReportCount {
        Long getReviewNo();
        long getCount();
    }
}
