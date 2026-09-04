package com.section.common.system.entity;

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

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "front_password_reset_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FrontPasswordResetToken extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "password_reset_token_no") private Long id;
    @Column(name = "member_no", nullable = false) private Long memberNo;
    @Column(name = "token_hash", nullable = false, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "used_at") private LocalDateTime usedAt;

    public static FrontPasswordResetToken issue(long memberNo, String tokenHash, LocalDateTime expiresAt) {
        FrontPasswordResetToken token = new FrontPasswordResetToken();
        token.memberNo = memberNo;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        return token;
    }
    public boolean isUsable(LocalDateTime now) { return usedAt == null && expiresAt.isAfter(now); }
    public void use(LocalDateTime now) { this.usedAt = now; }
}
