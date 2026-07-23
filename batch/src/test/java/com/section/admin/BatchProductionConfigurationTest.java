package com.section.admin;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BatchProductionConfigurationTest {

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
                .contains("max-http-request-header-size: ${SERVER_MAX_HTTP_REQUEST_HEADER_SIZE:16KB}")
                .contains("level: \"%5p [requestId:%X{requestId:-}]\"")
                .contains("check-template-location: false")
                .contains("size: ${BATCH_SCHEDULING_POOL_SIZE:1}")
                .contains("thread-name-prefix: batch-scheduler-")
                .contains("await-termination: true")
                .contains("await-termination-period: ${BATCH_SHUTDOWN_TIMEOUT:30s}")
                .contains("enabled: ${BATCH_DOCUMENT_STATS_ENABLED:false}")
                .contains("cron: ${BATCH_DOCUMENT_STATS_CRON:0 */10 * * * *}")
                .contains("enabled: ${BATCH_CONTENT_VIEW_RETENTION_ENABLED:false}")
                .contains("cron: ${BATCH_CONTENT_VIEW_RETENTION_CRON:0 30 3 * * *}")
                .contains("days: ${BATCH_CONTENT_VIEW_RETENTION_DAYS:180}")
                .contains("include-stacktrace: never");
    }
}
