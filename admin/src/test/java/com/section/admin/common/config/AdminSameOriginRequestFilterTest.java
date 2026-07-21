package com.section.admin.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSameOriginRequestFilterTest {

    private final AdminSameOriginRequestFilter filter = new AdminSameOriginRequestFilter();

    @Test
    @DisplayName("동일 출처 관리자 변경 요청은 허용한다")
    void allowsSameOriginMutation() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/admin/product/set");
        request.addHeader("Origin", "https://admin.grade-stock.test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] invoked = {false};

        filter.doFilter(request, response, (req, res) -> invoked[0] = true);

        assertTrue(invoked[0]);
    }

    @Test
    @DisplayName("외부 출처 관리자 API 요청은 403 계약으로 차단한다")
    void rejectsCrossOriginApiMutation() throws Exception {
        MockHttpServletRequest request = request("PATCH", "/api/admin/orders/status");
        request.addHeader("Origin", "https://attacker.test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("A005"));
    }

    @Test
    @DisplayName("출처가 없는 관리자 로그인 POST는 차단한다")
    void rejectsLoginWithoutOriginEvidence() throws Exception {
        MockHttpServletRequest request = request("POST", "/admin/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("조회 요청은 출처 헤더 없이도 통과한다")
    void allowsSafeMethod() throws Exception {
        MockHttpServletRequest request = request("GET", "/admin/dashboard");

        assertTrue(filter.shouldNotFilter(request));
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setScheme("https");
        request.setServerName("admin.grade-stock.test");
        request.setServerPort(443);
        return request;
    }
}
