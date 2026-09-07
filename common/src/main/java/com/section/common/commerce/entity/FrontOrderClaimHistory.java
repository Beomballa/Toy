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
@Table(name = "front_order_claim_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FrontOrderClaimHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_claim_history_no")
    private Long id;

    @Column(name = "order_claim_no", nullable = false)
    private Long claimNo;

    @Column(name = "before_status", length = 20)
    private String beforeStatus;

    @Column(name = "after_status", nullable = false, length = 20)
    private String afterStatus;

    @Column(name = "memo", length = 1000)
    private String memo;

    public static FrontOrderClaimHistory create(
            long claimNo,
            FrontOrderClaimStatus beforeStatus,
            FrontOrderClaimStatus afterStatus,
            String memo
    ) {
        FrontOrderClaimHistory history = new FrontOrderClaimHistory();
        history.claimNo = claimNo;
        history.beforeStatus = beforeStatus == null ? null : beforeStatus.name();
        history.afterStatus = afterStatus.name();
        history.memo = memo;
        return history;
    }
}
