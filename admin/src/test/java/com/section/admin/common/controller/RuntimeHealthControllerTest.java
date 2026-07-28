package com.section.admin.common.controller;

import com.section.common.system.controller.RuntimeHealthController;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuntimeHealthControllerTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final RuntimeHealthController controller = new RuntimeHealthController(jdbcTemplate);

    @Test
    void livenessDoesNotDependOnDatabase() {
        assertEquals("UP", controller.live().status());
    }

    @Test
    void readinessIsUpWhenDatabaseResponds() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        var response = controller.ready();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().status());
    }

    @Test
    void readinessHidesDatabaseFailureAndReturnsServiceUnavailable() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new IllegalStateException("database credentials must not leak"));

        var response = controller.ready();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("OUT_OF_SERVICE", response.getBody().status());
    }

    @Test
    void readinessIsUnavailableWhenRequiredSecurityTableIsMissing() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(jdbcTemplate.queryForList("SELECT rate_key FROM request_rate_limit_bucket WHERE 1 = 0"))
                .thenThrow(new IllegalStateException("missing table"));

        var response = controller.ready();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("OUT_OF_SERVICE", response.getBody().status());
    }
}
