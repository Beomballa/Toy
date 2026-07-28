package com.section.common.system.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RuntimeHealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health/live")
    public RuntimeHealthResponse live() {
        return new RuntimeHealthResponse("UP");
    }

    @GetMapping("/health/ready")
    public ResponseEntity<RuntimeHealthResponse> ready() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (Integer.valueOf(1).equals(result)) {
                jdbcTemplate.queryForList("SELECT rate_key FROM request_rate_limit_bucket WHERE 1 = 0");
                return ResponseEntity.ok(new RuntimeHealthResponse("UP"));
            }
        } catch (RuntimeException ignored) {
            // DB 예외 상세는 응답에 노출하지 않고 readiness 상태로만 전달합니다.
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new RuntimeHealthResponse("OUT_OF_SERVICE"));
    }

    public record RuntimeHealthResponse(String status) {
    }
}
