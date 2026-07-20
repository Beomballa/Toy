package com.section.front.system.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontSecurityHeadersFilterTest {

    private final FrontSecurityHeadersFilter filter = new FrontSecurityHeadersFilter();

    @Test
    @DisplayName("프론트 응답은 inline script 실행과 iframe 삽입을 차단한다")
    void addsBrowserSecurityHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String csp = response.getHeader("Content-Security-Policy");
        assertTrue(csp.contains("script-src 'self'"));
        assertTrue(csp.contains("frame-ancestors 'none'"));
        assertFalse(csp.contains("'unsafe-inline'"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", response.getHeader("X-Frame-Options"));
        assertNull(response.getHeader("Strict-Transport-Security"));
    }

    @Test
    @DisplayName("HTTPS 프론트 응답은 HSTS를 제공한다")
    void addsHstsForSecureRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertEquals("max-age=31536000; includeSubDomains", response.getHeader("Strict-Transport-Security"));
    }
}
