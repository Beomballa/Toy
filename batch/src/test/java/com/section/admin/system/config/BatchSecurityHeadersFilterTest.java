package com.section.admin.system.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class BatchSecurityHeadersFilterTest {

    private final BatchSecurityHeadersFilter filter = new BatchSecurityHeadersFilter();

    @Test
    @DisplayName("배치 HTTP 응답은 캐시와 브라우저 포함을 차단한다")
    void addsSecurityAndNoStoreHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health/live");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader("Content-Security-Policy")).contains("default-src 'none'");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("Cross-Origin-Resource-Policy")).isEqualTo("same-origin");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store, max-age=0");
    }

    @Test
    @DisplayName("HTTPS 배치 응답은 HSTS를 제공한다")
    void addsHstsForSecureRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health/live");
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains");
    }
}
