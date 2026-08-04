package com.section.front.auth.support;

import com.section.common.system.ratelimit.RateLimitStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class FrontLoginAttemptGuard {

    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_EMAIL_LENGTH = 255;

    private final RateLimitStore rateLimitStore;
    private final int maxFailures;
    private final int maxIpFailures;
    private final Duration lockDuration;
    private final Clock clock;

    @Autowired
    public FrontLoginAttemptGuard(
            RateLimitStore rateLimitStore,
            @Value("${front.auth.max-login-failures:5}") int maxFailures,
            @Value("${front.auth.max-ip-login-failures:25}") int maxIpFailures,
            @Value("${front.auth.login-lock-duration:15m}") Duration lockDuration
    ) {
        this(rateLimitStore, maxFailures, maxIpFailures, lockDuration, Clock.systemUTC());
    }

    FrontLoginAttemptGuard(
            RateLimitStore rateLimitStore,
            int maxFailures,
            int maxIpFailures,
            Duration lockDuration,
            Clock clock
    ) {
        this.rateLimitStore = rateLimitStore;
        this.maxFailures = Math.max(1, maxFailures);
        this.maxIpFailures = Math.max(this.maxFailures, maxIpFailures);
        this.lockDuration = lockDuration.isNegative() || lockDuration.isZero() ? Duration.ofMinutes(15) : lockDuration;
        this.clock = clock;
    }

    public boolean isBlocked(String ipAddress, String email) {
        return rateLimitStore.isBlocked(accountKey(ipAddress, email), clock.instant())
                || rateLimitStore.isBlocked(ipKey(ipAddress), clock.instant());
    }

    public void recordFailure(String ipAddress, String email) {
        rateLimitStore.recordFailure(accountKey(ipAddress, email), maxFailures, lockDuration, clock.instant());
        rateLimitStore.recordFailure(ipKey(ipAddress), maxIpFailures, lockDuration, clock.instant());
    }

    public void clear(String ipAddress, String email) {
        rateLimitStore.clear(accountKey(ipAddress, email));
    }

    private String accountKey(String ipAddress, String email) {
        return hashedKey("FRONT_AUTH_ACCOUNT", normalizeIp(ipAddress) + '|' + normalizeEmail(email));
    }

    private String ipKey(String ipAddress) {
        return hashedKey("FRONT_AUTH_IP", normalizeIp(ipAddress));
    }

    private String hashedKey(String scope, String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return scope + '|' + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("로그인 제한 키를 생성할 수 없습니다.", exception);
        }
    }

    private String normalizeIp(String ipAddress) {
        String normalized = ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress.trim();
        return truncate(normalized, MAX_IP_LENGTH);
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return truncate(normalized, MAX_EMAIL_LENGTH);
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
