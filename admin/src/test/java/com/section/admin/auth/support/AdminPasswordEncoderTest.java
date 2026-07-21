package com.section.admin.auth.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

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

    @Test
    @DisplayName("변조되거나 불완전한 PBKDF2 문자열은 거부한다")
    void rejectsMalformedEncodedPassword() {
        assertFalse(encoder.matches("admin1234", "{pbkdf2}not-a-number$bad$bad"));
        assertFalse(encoder.isEncoded("{pbkdf2}120000$bad$bad"));
        assertFalse(encoder.isEncoded("{pbkdf2}120000$only-two-parts"));
    }

    @Test
    @DisplayName("과도하거나 부족한 반복 횟수는 연산 전에 거부한다")
    void rejectsUnsafeIterationCount() {
        String salt = Base64.getEncoder().encodeToString(new byte[16]);
        String hash = Base64.getEncoder().encodeToString(new byte[32]);

        assertFalse(encoder.isEncoded("{pbkdf2}99999$" + salt + "$" + hash));
        assertFalse(encoder.isEncoded("{pbkdf2}600001$" + salt + "$" + hash));
        assertFalse(encoder.matches("admin1234", "{pbkdf2}999999999$" + salt + "$" + hash));
    }

    @Test
    @DisplayName("salt와 hash 길이가 규격과 다르면 거부한다")
    void rejectsInvalidSaltAndHashLength() {
        String shortSalt = Base64.getEncoder().encodeToString(new byte[8]);
        String salt = Base64.getEncoder().encodeToString(new byte[16]);
        String shortHash = Base64.getEncoder().encodeToString(new byte[16]);
        String hash = Base64.getEncoder().encodeToString(new byte[32]);

        assertFalse(encoder.isEncoded("{pbkdf2}120000$" + shortSalt + "$" + hash));
        assertFalse(encoder.isEncoded("{pbkdf2}120000$" + salt + "$" + shortHash));
    }

    @Test
    @DisplayName("현재 기준보다 낮은 유효 반복 횟수는 재해시 대상으로 판단한다")
    void detectsLowerCostHash() {
        String salt = Base64.getEncoder().encodeToString(new byte[16]);
        String hash = Base64.getEncoder().encodeToString(new byte[32]);

        assertTrue(encoder.needsRehash("{pbkdf2}100000$" + salt + "$" + hash));
        assertFalse(encoder.needsRehash("{pbkdf2}120000$" + salt + "$" + hash));
    }
}
