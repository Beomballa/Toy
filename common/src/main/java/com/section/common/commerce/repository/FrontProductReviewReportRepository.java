package com.section.common.commerce.repository;

import com.section.common.commerce.entity.FrontProductReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FrontProductReviewReportRepository extends JpaRepository<FrontProductReviewReport, Long> {

    boolean existsByReviewNoAndMemberNo(long reviewNo, long memberNo);
}
