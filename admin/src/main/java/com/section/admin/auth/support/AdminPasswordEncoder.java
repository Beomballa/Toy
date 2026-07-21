package com.section.admin.auth.support;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AdminPasswordEncoder {

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
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = derive(rawPassword.toCharArray(), salt, ITERATIONS);
        return PREFIX + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        if (!encodedPassword.startsWith(PREFIX)) {
            return MessageDigest.isEqual(
                    rawPassword.getBytes(StandardCharsets.UTF_8),
                    encodedPassword.getBytes(StandardCharsets.UTF_8)
            );
        }

        EncodedPassword parsed = parse(encodedPassword);
        if (parsed == null || rawPassword.length() > MAX_PASSWORD_LENGTH) {
            consumeDummyMatch(truncatePassword(rawPassword));
            return false;
        }
        byte[] actual = derive(rawPassword.toCharArray(), parsed.salt(), parsed.iterations());
        return MessageDigest.isEqual(actual, parsed.hash());
    }

    public boolean isEncoded(String password) {
        return parse(password) != null;
    }

    public boolean needsRehash(String password) {
        EncodedPassword parsed = parse(password);
        return parsed != null && parsed.iterations() < ITERATIONS;
    }

    public void consumeDummyMatch(String rawPassword) {
        derive(rawPassword.toCharArray(), new byte[SALT_BYTES], ITERATIONS);
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

    private String truncatePassword(String rawPassword) {
        return rawPassword.substring(0, Math.min(rawPassword.length(), MAX_PASSWORD_LENGTH));
    }

    private byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("관리자 비밀번호를 안전하게 처리할 수 없습니다.", e);
        } finally {
            spec.clearPassword();
        }
    }

    private record EncodedPassword(int iterations, byte[] salt, byte[] hash) {
    }
}
