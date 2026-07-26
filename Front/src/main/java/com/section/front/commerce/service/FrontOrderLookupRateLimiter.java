package com.section.front.commerce.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FrontOrderLookupRateLimiter {

    private static final int DEFAULT_MAX_ATTEMPTS = 10;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(5);
    private static final long CLEANUP_INTERVAL = 256;

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();
    private final Clock clock;
    private final int maxAttempts;
    private final Duration window;

    public FrontOrderLookupRateLimiter() {
        this(Clock.systemUTC(), DEFAULT_MAX_ATTEMPTS, DEFAULT_WINDOW);
    }

    FrontOrderLookupRateLimiter(Clock clock, int maxAttempts, Duration window) {
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    public void checkAndRecord(String clientAddress) {
        Instant now = clock.instant();
        String key = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress;
        AttemptWindow current = attempts.compute(key, (ignored, previous) -> {
            if (previous == null || !now.isBefore(previous.startedAt().plus(window))) {
                return new AttemptWindow(now, 1);
            }
            return new AttemptWindow(previous.startedAt(), previous.count() + 1);
        });

        if (requestCount.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            attempts.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().startedAt().plus(window)));
        }
        if (current.count() > maxAttempts) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "주문 조회 요청이 많습니다. 잠시 후 다시 시도해주세요."
            );
        }
    }

    private record AttemptWindow(Instant startedAt, int count) {
    }
}
