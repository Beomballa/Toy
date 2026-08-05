package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "front_member_product_activity",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_front_member_activity",
                columnNames = {"member_no", "activity_type", "product_no"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FrontMemberProductActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_no")
    private Long id;

    @Column(name = "member_no", nullable = false)
    private Long memberNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 20)
    private FrontMemberActivityType activityType;

    @Column(name = "product_no", nullable = false)
    private Long productNo;

    @Column(name = "last_interacted_at", nullable = false)
    private LocalDateTime lastInteractedAt;

    public static FrontMemberProductActivity create(
            long memberNo,
            FrontMemberActivityType activityType,
            long productNo,
            LocalDateTime lastInteractedAt
    ) {
        FrontMemberProductActivity activity = new FrontMemberProductActivity();
        activity.memberNo = memberNo;
        activity.activityType = activityType;
        activity.productNo = productNo;
        activity.lastInteractedAt = lastInteractedAt;
        return activity;
    }
}
