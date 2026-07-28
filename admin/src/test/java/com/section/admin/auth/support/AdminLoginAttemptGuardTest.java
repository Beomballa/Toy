package com.section.admin.auth.support;

import com.section.common.system.ratelimit.InMemoryRateLimitStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminLoginAttemptGuardTest {

    @Test
    @DisplayName("설정된 실패 횟수에 도달하면 로그인을 잠근다")
    void blocksAfterMaximumFailures() {
        AdminLoginAttemptGuard guard = new AdminLoginAttemptGuard(
                new InMemoryRateLimitStore(),
                3, Duration.ofMinutes(15), Clock.fixed(Instant.parse("2026-07-20T05:00:00Z"), ZoneOffset.UTC)
        );

        guard.recordFailure("127.0.0.1", "Master");
        guard.recordFailure("127.0.0.1", "master");
        assertFalse(guard.isBlocked("127.0.0.1", "MASTER"));

        guard.recordFailure("127.0.0.1", "master");
        assertTrue(guard.isBlocked("127.0.0.1", "master"));
    }

    @Test
    @DisplayName("로그인 성공 시 누적 실패 횟수를 초기화한다")
    void clearsFailuresAfterSuccess() {
        AdminLoginAttemptGuard guard = new AdminLoginAttemptGuard(
                new InMemoryRateLimitStore(),
                2,
                Duration.ofMinutes(15),
                Clock.systemUTC()
        );
        guard.recordFailure("127.0.0.1", "master");
        guard.recordFailure("127.0.0.1", "master");
        assertTrue(guard.isBlocked("127.0.0.1", "master"));

        guard.clear("127.0.0.1", "master");
        assertFalse(guard.isBlocked("127.0.0.1", "master"));
    }

    @Test
    @DisplayName("동일 IP에서 여러 계정을 시도해도 IP 단위로 차단한다")
    void blocksDistributedLoginIdsByIp() {
        AdminLoginAttemptGuard guard = new AdminLoginAttemptGuard(
                new InMemoryRateLimitStore(),
                3, 4, Duration.ofMinutes(15), Clock.systemUTC(), 100
        );

        guard.recordFailure("127.0.0.1", "admin-1");
        guard.recordFailure("127.0.0.1", "admin-2");
        guard.recordFailure("127.0.0.1", "admin-3");
        guard.recordFailure("127.0.0.1", "admin-4");

        assertTrue(guard.isBlocked("127.0.0.1", "new-admin"));
        assertFalse(guard.isBlocked("127.0.0.2", "new-admin"));
    }

    @Test
    @DisplayName("실패 시간 창이 지나면 부분 누적 횟수를 초기화한다")
    void resetsPartialFailuresAfterWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-20T05:00:00Z"));
        AdminLoginAttemptGuard guard = new AdminLoginAttemptGuard(
                new InMemoryRateLimitStore(),
                2,
                10,
                Duration.ofMinutes(15),
                clock,
                100
        );
        guard.recordFailure("127.0.0.1", "master");

        clock.advance(Duration.ofMinutes(16));
        guard.recordFailure("127.0.0.1", "master");

        assertFalse(guard.isBlocked("127.0.0.1", "master"));
    }

    @Test
    @DisplayName("추적 엔트리는 설정된 최대 크기를 넘지 않는다")
    void boundsTrackedAttemptEntries() {
        AdminLoginAttemptGuard guard = new AdminLoginAttemptGuard(
                new InMemoryRateLimitStore(6),
                10, 50, Duration.ofMinutes(15), Clock.systemUTC(), 6
        );

        for (int index = 0; index < 10; index++) {
            guard.recordFailure("127.0.0." + index, "admin-" + index);
        }

        assertTrue(guard.trackedAttemptCount() <= 6);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
