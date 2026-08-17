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
                .contains("ddl-auto: ${JPA_DDL_AUTO:none}")
                .contains("show-sql: false")
                .contains("open-in-view: false")
                .contains("shutdown: graceful")
                .contains("forward-headers-strategy: framework")
                .contains("max-http-request-header-size: ${SERVER_MAX_HTTP_REQUEST_HEADER_SIZE:16KB}")
                .contains("level: \"%5p [requestId:%X{requestId:-}]\"")
                .contains("compression:")
                .contains("min-response-size: ${SERVER_COMPRESSION_MIN_SIZE:1024}")
                .contains("max-age: ${STATIC_CACHE_MAX_AGE:365d}")
                .contains("cache-public: true")
                .contains("timeout: ${FRONT_SESSION_TIMEOUT:30m}")
                .contains("secure: ${FRONT_SESSION_COOKIE_SECURE:true}")
                .contains("same-site: lax")
                .contains("max-login-failures: ${FRONT_MAX_LOGIN_FAILURES:5}")
                .contains("max-ip-login-failures: ${FRONT_MAX_IP_LOGIN_FAILURES:25}")
                .contains("login-lock-duration: ${FRONT_LOGIN_LOCK_DURATION:15m}")
                .contains("include-message: never")
                .contains("include-stacktrace: never");
    }
}
