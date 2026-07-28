package com.section.front.commerce.service;

import com.section.common.system.ratelimit.RateLimitStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;

@Component
public class FrontOrderLookupRateLimiter {

    private static final int MAX_CLIENT_ADDRESS_LENGTH = 64;
    private final RateLimitStore rateLimitStore;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration window;

    @Autowired
    public FrontOrderLookupRateLimiter(
            RateLimitStore rateLimitStore,
            @Value("${front.order-lookup.max-attempts:10}") int maxAttempts,
            @Value("${front.order-lookup.window:5m}") Duration window
    ) {
        this(rateLimitStore, Clock.systemUTC(), maxAttempts, window);
    }

    FrontOrderLookupRateLimiter(RateLimitStore rateLimitStore, Clock clock, int maxAttempts, Duration window) {
        this.rateLimitStore = rateLimitStore;
        this.clock = clock;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.window = window.isNegative() || window.isZero() ? Duration.ofMinutes(5) : window;
    }

    public void checkAndRecord(String clientAddress) {
        String normalizedAddress = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress.trim();
        String key = "FRONT_ORDER_LOOKUP|" + normalizedAddress.substring(
                0,
                Math.min(normalizedAddress.length(), MAX_CLIENT_ADDRESS_LENGTH)
        );
        if (rateLimitStore.recordFailure(key, maxAttempts + 1, window, clock.instant())) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "주문 조회 요청이 많습니다. 잠시 후 다시 시도해주세요."
            );
        }
    }
}
