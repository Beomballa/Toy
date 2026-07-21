package com.section.admin.auth.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AdminLoginAttemptGuard {

    private static final int MAX_TRACKED_ATTEMPTS = 10_000;
    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_LOGIN_ID_LENGTH = 50;

    private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final int maxIpFailures;
    private final Duration lockDuration;
    private final Clock clock;
    private final int maxTrackedAttempts;

    @Autowired
    public AdminLoginAttemptGuard(
            @Value("${admin.auth.max-login-failures:5}") int maxFailures,
            @Value("${admin.auth.max-ip-login-failures:25}") int maxIpFailures,
            @Value("${admin.auth.login-lock-duration:15m}") Duration lockDuration
    ) {
        this(maxFailures, maxIpFailures, lockDuration, Clock.systemUTC(), MAX_TRACKED_ATTEMPTS);
    }

    AdminLoginAttemptGuard(int maxFailures, Duration lockDuration, Clock clock) {
        this(maxFailures, Math.max(5, maxFailures * 5), lockDuration, clock, MAX_TRACKED_ATTEMPTS);
    }

    AdminLoginAttemptGuard(
            int maxFailures,
            int maxIpFailures,
            Duration lockDuration,
            Clock clock,
            int maxTrackedAttempts
    ) {
        this.maxFailures = Math.max(1, maxFailures);
        this.maxIpFailures = Math.max(this.maxFailures, maxIpFailures);
        this.lockDuration = lockDuration.isNegative() || lockDuration.isZero() ? Duration.ofMinutes(15) : lockDuration;
        this.clock = clock;
        this.maxTrackedAttempts = Math.max(2, maxTrackedAttempts);
    }

    public boolean isBlocked(String ipAddress, String loginId) {
        Instant now = clock.instant();
        return isBlocked(accountKey(ipAddress, loginId), now) || isBlocked(ipKey(ipAddress), now);
    }

    private boolean isBlocked(String attemptKey, Instant now) {
        AttemptState state = attempts.get(attemptKey);
        if (state == null) {
            return false;
        }
        if (state.blockedUntil() != null && state.blockedUntil().isAfter(now)) {
            return true;
        }
        if (state.lastFailureAt().plus(lockDuration).isBefore(now) || state.blockedUntil() != null) {
            attempts.remove(attemptKey, state);
        }
        return false;
    }

    public void recordFailure(String ipAddress, String loginId) {
        Instant now = clock.instant();
        recordFailure(accountKey(ipAddress, loginId), maxFailures, now);
        recordFailure(ipKey(ipAddress), maxIpFailures, now);
        evictExpiredEntries(now);
    }

    public void clear(String ipAddress, String loginId) {
        attempts.remove(accountKey(ipAddress, loginId));
        attempts.remove(ipKey(ipAddress));
    }

    private void recordFailure(String key, int failureLimit, Instant now) {
        attempts.compute(key, (attemptKey, state) -> {
            boolean expired = state != null && state.lastFailureAt().plus(lockDuration).isBefore(now);
            int failures = state == null || expired ? 1 : state.failures() + 1;
            Instant blockedUntil = failures >= failureLimit ? now.plus(lockDuration) : null;
            return new AttemptState(failures, blockedUntil, now);
        });
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

    private void evictExpiredEntries(Instant now) {
        if (attempts.size() <= maxTrackedAttempts) {
            return;
        }
        Instant staleBefore = now.minus(lockDuration);
        attempts.entrySet().removeIf(entry -> entry.getValue().lastFailureAt().isBefore(staleBefore));
        int excess = attempts.size() - maxTrackedAttempts;
        if (excess <= 0) {
            return;
        }
        attempts.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().lastFailureAt()))
                .limit(excess)
                .forEach(entry -> attempts.remove(entry.getKey(), entry.getValue()));
    }

    int trackedAttemptCount() {
        return attempts.size();
    }

    private record AttemptState(int failures, Instant blockedUntil, Instant lastFailureAt) {
    }
}
