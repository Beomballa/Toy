package com.section.common.commerce.entity;

import com.section.common.base.entity.type.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Getter @Table(name = "front_order_claim") @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FrontOrderClaim extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "order_claim_no") private Long id;
    @Column(name = "order_no", nullable = false) private Long orderNo;
    @Column(name = "member_no", nullable = false) private Long memberNo;
    @Enumerated(EnumType.STRING) @Column(name = "claim_type", nullable = false, length = 20) private FrontOrderClaimType claimType;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20) private FrontOrderClaimStatus status;
    @Column(name = "reason", nullable = false, length = 1000) private String reason;
    @Column(name = "requested_at", nullable = false) private LocalDateTime requestedAt;
    @Column(name = "resolved_at") private LocalDateTime resolvedAt;
    public static FrontOrderClaim request(long orderNo, long memberNo, FrontOrderClaimType type, String reason, LocalDateTime now) {
        FrontOrderClaim claim = new FrontOrderClaim(); claim.orderNo=orderNo; claim.memberNo=memberNo; claim.claimType=type; claim.status=FrontOrderClaimStatus.REQUESTED; claim.reason=reason; claim.requestedAt=now; return claim;
    }
    public boolean isOpen() { return status == FrontOrderClaimStatus.REQUESTED || status == FrontOrderClaimStatus.APPROVED; }

    public void changeStatus(FrontOrderClaimStatus nextStatus, LocalDateTime now) {
        this.status = nextStatus;
        this.resolvedAt = nextStatus == FrontOrderClaimStatus.REJECTED || nextStatus == FrontOrderClaimStatus.COMPLETED ? now : null;
    }
}
