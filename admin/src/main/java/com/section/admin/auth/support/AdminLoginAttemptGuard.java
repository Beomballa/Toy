package com.section.admin.auth.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AdminLoginAttemptGuard {

    private static final int MAX_TRACKED_ATTEMPTS = 10_000;

    private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lockDuration;
    private final Clock clock;

    @Autowired
    public AdminLoginAttemptGuard(
            @Value("${admin.auth.max-login-failures:5}") int maxFailures,
            @Value("${admin.auth.login-lock-duration:15m}") Duration lockDuration
    ) {
        this(maxFailures, lockDuration, Clock.systemUTC());
    }

    AdminLoginAttemptGuard(int maxFailures, Duration lockDuration, Clock clock) {
        this.maxFailures = Math.max(1, maxFailures);
        this.lockDuration = lockDuration.isNegative() || lockDuration.isZero() ? Duration.ofMinutes(15) : lockDuration;
        this.clock = clock;
    }

    public boolean isBlocked(String ipAddress, String loginId) {
        String attemptKey = key(ipAddress, loginId);
        AttemptState state = attempts.get(attemptKey);
        if (state == null) {
            return false;
        }
        Instant now = clock.instant();
        if (state.blockedUntil() != null && state.blockedUntil().isAfter(now)) {
            return true;
        }
        if (state.blockedUntil() != null) {
            attempts.remove(attemptKey, state);
        }
        return false;
    }

    public void recordFailure(String ipAddress, String loginId) {
        Instant now = clock.instant();
        attempts.compute(key(ipAddress, loginId), (key, state) -> {
            int failures = state == null ? 1 : state.failures() + 1;
            Instant blockedUntil = failures >= maxFailures ? now.plus(lockDuration) : null;
            return new AttemptState(failures, blockedUntil, now);
        });
        evictExpiredEntries(now);
    }

    public void clear(String ipAddress, String loginId) {
        attempts.remove(key(ipAddress, loginId));
    }

    private String key(String ipAddress, String loginId) {
        String normalizedIp = ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress.trim();
        String normalizedLoginId = loginId == null ? "" : loginId.trim().toLowerCase(Locale.ROOT);
        return normalizedIp + '|' + normalizedLoginId;
    }

    private void evictExpiredEntries(Instant now) {
        if (attempts.size() <= MAX_TRACKED_ATTEMPTS) {
            return;
        }
        Instant staleBefore = now.minus(lockDuration);
        attempts.entrySet().removeIf(entry -> entry.getValue().lastFailureAt().isBefore(staleBefore));
    }

    private record AttemptState(int failures, Instant blockedUntil, Instant lastFailureAt) {
    }
}
