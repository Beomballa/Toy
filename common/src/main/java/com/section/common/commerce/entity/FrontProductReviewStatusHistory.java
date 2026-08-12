package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "front_product_review_status_history")
public class FrontProductReviewStatusHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_status_history_no")
    private Long id;

    @Column(name = "review_no", nullable = false)
    private Long reviewNo;

    @Column(name = "before_status", nullable = false, length = 20)
    private String beforeStatus;

    @Column(name = "after_status", nullable = false, length = 20)
    private String afterStatus;

    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;

    private FrontProductReviewStatusHistory(long reviewNo, String beforeStatus, String afterStatus, String actionType) {
        this.reviewNo = reviewNo;
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.actionType = actionType;
    }

    public static FrontProductReviewStatusHistory create(long reviewNo, String beforeStatus, String afterStatus) {
        String actionType = FrontProductReviewStatus.HIDDEN.name().equals(afterStatus) ? "HIDE" : "RESTORE";
        return new FrontProductReviewStatusHistory(reviewNo, beforeStatus, afterStatus, actionType);
    }
}
