package com.section.admin.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AdminProductionConfigurationTest {

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
                .contains("http-only: true")
                .contains("secure: true")
                .contains("same-site: strict")
                .contains("tracking-modes: cookie")
                .contains("name: GS_ADMIN_SESSION")
                .contains("max-login-failures: ${ADMIN_MAX_LOGIN_FAILURES:5}")
                .contains("login-lock-duration: ${ADMIN_LOGIN_LOCK_DURATION:15m}")
                .contains("login-id: ${ADMIN_BOOTSTRAP_LOGIN_ID:}")
                .contains("password: ${ADMIN_BOOTSTRAP_PASSWORD:}")
                .contains("include-stacktrace: never");
    }
}
