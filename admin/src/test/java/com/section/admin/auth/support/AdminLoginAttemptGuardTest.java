package com.section.admin.auth.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminLoginAttemptGuardTest {

    @Test
    @DisplayName("설정된 실패 횟수에 도달하면 로그인을 잠근다")
    void blocksAfterMaximumFailures() {
        AdminLoginAttemptGuard guard = new AdminLoginAttemptGuard(
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
        AdminLoginAttemptGuard guard = new AdminLoginAttemptGuard(2, Duration.ofMinutes(15), Clock.systemUTC());
        guard.recordFailure("127.0.0.1", "master");
        guard.recordFailure("127.0.0.1", "master");
        assertTrue(guard.isBlocked("127.0.0.1", "master"));

        guard.clear("127.0.0.1", "master");
        assertFalse(guard.isBlocked("127.0.0.1", "master"));
    }
}
