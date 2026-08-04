package com.section.front.auth.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FrontPasswordEncoderTest {

    private final FrontPasswordEncoder encoder = new FrontPasswordEncoder();

    @Test
    void encodesAndMatchesPasswordWithoutStoringRawValue() {
        String encoded = encoder.encode("noren1234");

        assertThat(encoded).startsWith("{pbkdf2}120000$").doesNotContain("noren1234");
        assertThat(encoder.matches("noren1234", encoded)).isTrue();
        assertThat(encoder.matches("wrong1234", encoded)).isFalse();
        assertThat(encoder.needsRehash(encoded)).isFalse();
    }

    @Test
    void acceptsLegacyPasswordOnlyForMigration() {
        assertThat(encoder.matches("legacy1234", "legacy1234")).isTrue();
        assertThat(encoder.needsRehash("legacy1234")).isTrue();
    }
}
