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
                .contains("include-stacktrace: never");
    }
}
