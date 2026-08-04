package com.section.front.auth.support;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class FrontPasswordEncoder {

    private static final String PREFIX = "{pbkdf2}";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int MIN_ITERATIONS = 100_000;
    private static final int MAX_ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int HASH_BYTES = KEY_BITS / Byte.SIZE;
    private static final int MAX_PASSWORD_LENGTH = 100;

    private final SecureRandom secureRandom = new SecureRandom();

    public String encode(String rawPassword) {
        validateRawPassword(rawPassword);
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = derive(rawPassword.toCharArray(), salt, ITERATIONS);
        return PREFIX + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || rawPassword.length() > MAX_PASSWORD_LENGTH) {
            consumeDummyMatch(rawPassword == null ? "" : rawPassword.substring(0, MAX_PASSWORD_LENGTH));
            return false;
        }
        if (!encodedPassword.startsWith(PREFIX)) {
            return MessageDigest.isEqual(
                    rawPassword.getBytes(StandardCharsets.UTF_8),
                    encodedPassword.getBytes(StandardCharsets.UTF_8)
            );
        }
        EncodedPassword parsed = parse(encodedPassword);
        if (parsed == null) {
            consumeDummyMatch(rawPassword);
            return false;
        }
        byte[] actual = derive(rawPassword.toCharArray(), parsed.salt(), parsed.iterations());
        return MessageDigest.isEqual(actual, parsed.hash());
    }

    public boolean needsRehash(String password) {
        EncodedPassword parsed = parse(password);
        return parsed == null || parsed.iterations() < ITERATIONS;
    }

    public void consumeDummyMatch(String rawPassword) {
        String normalized = rawPassword == null ? "" : rawPassword.substring(0, Math.min(rawPassword.length(), MAX_PASSWORD_LENGTH));
        derive(normalized.toCharArray(), new byte[SALT_BYTES], ITERATIONS);
    }

    private void validateRawPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty() || rawPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("비밀번호 길이가 올바르지 않습니다.");
        }
    }

    private EncodedPassword parse(String encodedPassword) {
        if (encodedPassword == null || !encodedPassword.startsWith(PREFIX)) {
            return null;
        }
        try {
            String[] parts = encodedPassword.substring(PREFIX.length()).split("\\$", -1);
            if (parts.length != 3) {
                return null;
            }
            int iterations = Integer.parseInt(parts[0]);
            if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
                return null;
            }
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] hash = Base64.getDecoder().decode(parts[2]);
            if (salt.length != SALT_BYTES || hash.length != HASH_BYTES) {
                return null;
            }
            return new EncodedPassword(iterations, salt, hash);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("회원 비밀번호를 안전하게 처리할 수 없습니다.", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private record EncodedPassword(int iterations, byte[] salt, byte[] hash) {
    }
}
