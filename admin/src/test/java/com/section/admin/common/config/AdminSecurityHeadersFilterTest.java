package com.section.admin.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSecurityHeadersFilterTest {

    private final AdminSecurityHeadersFilter filter = new AdminSecurityHeadersFilter();

    @Test
    @DisplayName("관리자 응답은 iframe 삽입과 브라우저 권한 사용을 차단한다")
    void addsAdminSecurityHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertEquals("DENY", response.getHeader("X-Frame-Options"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("no-referrer", response.getHeader("Referrer-Policy"));
        assertTrue(response.getHeader("Content-Security-Policy").contains("frame-ancestors 'none'"));
        assertNull(response.getHeader("Strict-Transport-Security"));
    }

    @Test
    @DisplayName("HTTPS 관리자 응답은 HSTS를 제공한다")
    void addsHstsForSecureAdminRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/login");
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertEquals("max-age=31536000; includeSubDomains", response.getHeader("Strict-Transport-Security"));
    }
}
