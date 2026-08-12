package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "front_product_review_report",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_front_product_review_report_member_review",
                columnNames = {"member_no", "review_no"}
        )
)
public class FrontProductReviewReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_report_no")
    private Long id;

    @Column(name = "review_no", nullable = false)
    private Long reviewNo;

    @Column(name = "member_no", nullable = false)
    private Long memberNo;

    @Column(name = "reason", nullable = false, length = 30)
    private String reason;

    @Column(name = "detail", length = 500)
    private String detail;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    private FrontProductReviewReport(long reviewNo, long memberNo, String reason, String detail) {
        this.reviewNo = reviewNo;
        this.memberNo = memberNo;
        this.reason = reason;
        this.detail = detail;
        this.status = FrontProductReviewReportStatus.PENDING.name();
    }

    public static FrontProductReviewReport create(long reviewNo, long memberNo, String reason, String detail) {
        return new FrontProductReviewReport(reviewNo, memberNo, reason, detail);
    }

    public void resolve() {
        this.status = FrontProductReviewReportStatus.RESOLVED.name();
    }

    public boolean isResolved() {
        return FrontProductReviewReportStatus.RESOLVED.name().equals(status);
    }
}
