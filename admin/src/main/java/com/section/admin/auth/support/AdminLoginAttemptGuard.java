package com.section.admin.auth.support;

import com.section.common.system.ratelimit.RateLimitStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

@Component
public class AdminLoginAttemptGuard {

    private static final int MAX_TRACKED_ATTEMPTS = 10_000;
    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_LOGIN_ID_LENGTH = 50;

    private final RateLimitStore rateLimitStore;
    private final int maxFailures;
    private final int maxIpFailures;
    private final Duration lockDuration;
    private final Clock clock;
    private final int maxTrackedAttempts;

    @Autowired
    public AdminLoginAttemptGuard(
            RateLimitStore rateLimitStore,
            @Value("${admin.auth.max-login-failures:5}") int maxFailures,
            @Value("${admin.auth.max-ip-login-failures:25}") int maxIpFailures,
            @Value("${admin.auth.login-lock-duration:15m}") Duration lockDuration
    ) {
        this(rateLimitStore, maxFailures, maxIpFailures, lockDuration, Clock.systemUTC(), MAX_TRACKED_ATTEMPTS);
    }

    AdminLoginAttemptGuard(RateLimitStore rateLimitStore, int maxFailures, Duration lockDuration, Clock clock) {
        this(rateLimitStore, maxFailures, Math.max(5, maxFailures * 5), lockDuration, clock, MAX_TRACKED_ATTEMPTS);
    }

    AdminLoginAttemptGuard(
            RateLimitStore rateLimitStore,
            int maxFailures,
            int maxIpFailures,
            Duration lockDuration,
            Clock clock,
            int maxTrackedAttempts
    ) {
        this.rateLimitStore = rateLimitStore;
        this.maxFailures = Math.max(1, maxFailures);
        this.maxIpFailures = Math.max(this.maxFailures, maxIpFailures);
        this.lockDuration = lockDuration.isNegative() || lockDuration.isZero() ? Duration.ofMinutes(15) : lockDuration;
        this.clock = clock;
        this.maxTrackedAttempts = Math.max(2, maxTrackedAttempts);
    }

    public boolean isBlocked(String ipAddress, String loginId) {
        return rateLimitStore.isBlocked(accountKey(ipAddress, loginId), clock.instant())
                || rateLimitStore.isBlocked(ipKey(ipAddress), clock.instant());
    }

    public void recordFailure(String ipAddress, String loginId) {
        rateLimitStore.recordFailure(accountKey(ipAddress, loginId), maxFailures, lockDuration, clock.instant());
        rateLimitStore.recordFailure(ipKey(ipAddress), maxIpFailures, lockDuration, clock.instant());
    }

    public void clear(String ipAddress, String loginId) {
        rateLimitStore.clear(accountKey(ipAddress, loginId));
        rateLimitStore.clear(ipKey(ipAddress));
    }

    private String accountKey(String ipAddress, String loginId) {
        return "ACCOUNT|" + normalizeIp(ipAddress) + '|' + normalizeLoginId(loginId);
    }

    private String ipKey(String ipAddress) {
        return "IP|" + normalizeIp(ipAddress);
    }

    private String normalizeIp(String ipAddress) {
        String normalized = ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress.trim();
        return truncate(normalized, MAX_IP_LENGTH);
    }

    private String normalizeLoginId(String loginId) {
        String normalized = loginId == null ? "" : loginId.trim().toLowerCase(Locale.ROOT);
        return truncate(normalized, MAX_LOGIN_ID_LENGTH);
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    int trackedAttemptCount() {
        return (int) Math.min(rateLimitStore.trackedCount(), maxTrackedAttempts);
    }
}
