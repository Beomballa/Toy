package com.section.admin.auth.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminPasswordEncoderTest {

    private final AdminPasswordEncoder encoder = new AdminPasswordEncoder();

    @Test
    @DisplayName("동일한 비밀번호도 매번 다른 솔트로 해시한다")
    void encodeUsesRandomSalt() {
        String first = encoder.encode("admin1234");
        String second = encoder.encode("admin1234");

        assertNotEquals(first, second);
        assertTrue(encoder.matches("admin1234", first));
        assertFalse(encoder.matches("wrong-password", first));
    }

    @Test
    @DisplayName("기존 평문 비밀번호는 마이그레이션 로그인을 위해 비교할 수 있다")
    void matchesLegacyPlainTextPassword() {
        assertTrue(encoder.matches("admin1234", "admin1234"));
        assertFalse(encoder.matches("wrong-password", "admin1234"));
    }
}
