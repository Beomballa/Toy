package com.section.admin.common.config;

import com.section.common.system.support.RequestIdSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AdminRequestIdFilterTest {

    private final AdminRequestIdFilter filter = new AdminRequestIdFilter();

    @Test
    @DisplayName("관리자 요청 ID를 응답과 로그 컨텍스트에 연결한다")
    void linksRequestIdToResponseAndLoggingContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/dashboard");
        request.addHeader(RequestIdSupport.HEADER_NAME, "admin-request-2001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> contextRequestId = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> contextRequestId.set(MDC.get(RequestIdSupport.MDC_KEY)));

        assertThat(response.getHeader(RequestIdSupport.HEADER_NAME)).isEqualTo("admin-request-2001");
        assertThat(contextRequestId.get()).isEqualTo("admin-request-2001");
        assertThat(MDC.get(RequestIdSupport.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("출처가 차단된 관리자 API 응답도 요청 ID를 제공한다")
    void addsRequestIdToRejectedMutation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/products");
        request.setScheme("https");
        request.setServerName("admin.grade-stock.test");
        request.setServerPort(443);
        request.addHeader("Origin", "https://attacker.test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AdminSameOriginRequestFilter sameOriginFilter = new AdminSameOriginRequestFilter();

        filter.doFilter(request, response, (req, res) -> sameOriginFilter.doFilter(req, res, (nextReq, nextRes) -> { }));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader(RequestIdSupport.HEADER_NAME)).matches("[a-f0-9]{32}");
        assertThat(response.getContentAsString()).contains("A005");
    }
}
