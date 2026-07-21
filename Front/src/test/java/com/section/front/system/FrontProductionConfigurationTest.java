package com.section.front.system;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FrontProductionConfigurationTest {

    @Test
    void productionProfileRequiresDatabaseAndProtectsRuntimeDetails() throws IOException {
        String config = new ClassPathResource("application-prod.yml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(config)
                .contains("url: ${DB_URL}")
                .contains("username: ${DB_USERNAME}")
                .contains("password: ${DB_PASSWORD}")
                .contains("show-sql: false")
                .contains("shutdown: graceful")
                .contains("compression:")
                .contains("min-response-size: ${SERVER_COMPRESSION_MIN_SIZE:1024}")
                .contains("max-age: ${STATIC_CACHE_MAX_AGE:365d}")
                .contains("cache-public: true")
                .contains("include-stacktrace: never");
    }
}
