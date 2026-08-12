package com.section.common.commerce.repository;

import com.section.common.commerce.entity.FrontProductReviewReport;
import com.section.common.commerce.entity.FrontProductReviewReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FrontProductReviewReportRepository extends JpaRepository<FrontProductReviewReport, Long> {

    boolean existsByReviewNoAndMemberNo(long reviewNo, long memberNo);

    boolean existsByReviewNo(long reviewNo);

    void deleteByReviewNo(long reviewNo);

    List<FrontProductReviewReport> findAllByReviewNoInOrderByIdDesc(List<Long> reviewNos);

    List<FrontProductReviewReport> findAllByReviewNoAndStatus(long reviewNo, String status);

    @Query("select report.reviewNo as reviewNo, count(report) as count from FrontProductReviewReport report "
            + "where report.reviewNo in :reviewNos group by report.reviewNo")
    List<ReviewReportCount> countByReviewNoIn(@Param("reviewNos") List<Long> reviewNos);

    @Query("select report.reviewNo as reviewNo, count(report) as count from FrontProductReviewReport report "
            + "where report.reviewNo in :reviewNos and report.status = :status group by report.reviewNo")
    List<ReviewReportCount> countByReviewNoInAndStatus(
            @Param("reviewNos") List<Long> reviewNos,
            @Param("status") String status
    );

    interface ReviewReportCount {
        Long getReviewNo();
        long getCount();
    }
}
