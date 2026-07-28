package com.section.front.commerce.service;

import com.section.common.system.ratelimit.InMemoryRateLimitStore;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrontOrderLookupRateLimiterTest {

    @Test
    void blocksRequestsOverTheConfiguredWindowLimit() {
        FrontOrderLookupRateLimiter limiter = new FrontOrderLookupRateLimiter(
                new InMemoryRateLimitStore(),
                Clock.fixed(Instant.parse("2026-07-26T01:00:00Z"), ZoneOffset.UTC),
                2,
                Duration.ofMinutes(5)
        );

        limiter.checkAndRecord("192.0.2.10");
        limiter.checkAndRecord("192.0.2.10");

        assertThatThrownBy(() -> limiter.checkAndRecord("192.0.2.10"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("429");
        assertThatCode(() -> limiter.checkAndRecord("192.0.2.11"))
                .doesNotThrowAnyException();
    }
}
