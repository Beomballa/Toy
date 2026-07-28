package com.section.common.system.ratelimit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "request_rate_limit_bucket")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RateLimitBucket {

    @Id
    @Column(name = "rate_key", length = 160)
    private String key;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "window_started_at", nullable = false)
    private LocalDateTime windowStartedAt;

    @Column(name = "last_attempt_at", nullable = false)
    private LocalDateTime lastAttemptAt;

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    public boolean isBlocked(LocalDateTime now) {
        return blockedUntil != null && blockedUntil.isAfter(now);
    }

    public boolean recordFailure(LocalDateTime now, int failureLimit, Duration window) {
        if (!lastAttemptAt.plus(window).isAfter(now) || blockedUntil != null && !blockedUntil.isAfter(now)) {
            attemptCount = 0;
            windowStartedAt = now;
            blockedUntil = null;
        }
        attemptCount++;
        lastAttemptAt = now;
        if (attemptCount >= failureLimit) {
            blockedUntil = now.plus(window);
        }
        return isBlocked(now);
    }
}
