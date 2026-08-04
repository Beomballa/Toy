package com.section.front.auth.support;

import com.section.common.system.ratelimit.InMemoryRateLimitStore;
import com.section.common.system.ratelimit.RateLimitStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FrontLoginAttemptGuardTest {

    @Test
    void blocksRepeatedAccountFailuresAndClearsOnlySuccessfulAccount() {
        FrontLoginAttemptGuard guard = new FrontLoginAttemptGuard(
                new InMemoryRateLimitStore(),
                2,
                10,
                Duration.ofMinutes(15),
                Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneOffset.UTC)
        );

        guard.recordFailure("192.0.2.1", "MEMBER@EXAMPLE.COM");
        assertThat(guard.isBlocked("192.0.2.1", "member@example.com")).isFalse();
        guard.recordFailure("192.0.2.1", "member@example.com");
        assertThat(guard.isBlocked("192.0.2.1", "member@example.com")).isTrue();

        guard.clear("192.0.2.1", "member@example.com");
        assertThat(guard.isBlocked("192.0.2.1", "member@example.com")).isFalse();
    }

    @Test
    void hashesLongLoginIdentifiersIntoBoundedPersistentKeys() {
        CapturingRateLimitStore store = new CapturingRateLimitStore();
        FrontLoginAttemptGuard guard = new FrontLoginAttemptGuard(
                store,
                5,
                25,
                Duration.ofMinutes(15),
                Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneOffset.UTC)
        );

        guard.recordFailure("2001:db8:ffff:ffff:ffff:ffff:ffff:ffff", "a".repeat(240) + "@example.com");

        assertThat(store.keys)
                .hasSize(2)
                .allSatisfy(key -> {
                    assertThat(key).hasSizeLessThanOrEqualTo(160);
                    assertThat(key).doesNotContain("example.com", "2001:db8");
                });
    }

    private static class CapturingRateLimitStore implements RateLimitStore {
        private final List<String> keys = new ArrayList<>();

        @Override
        public boolean isBlocked(String key, Instant now) {
            return false;
        }

        @Override
        public boolean recordFailure(String key, int failureLimit, Duration window, Instant now) {
            keys.add(key);
            return false;
        }

        @Override
        public void clear(String key) {
        }

        @Override
        public long trackedCount() {
            return keys.size();
        }
    }
}
