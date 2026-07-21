package com.section.front.system.config;

import com.section.common.system.support.RequestIdSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FrontRequestIdFilterTest {

    private final FrontRequestIdFilter filter = new FrontRequestIdFilter();

    @Test
    @DisplayName("유효한 요청 ID는 응답과 로그 컨텍스트에서 재사용한다")
    void reusesSafeRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(RequestIdSupport.HEADER_NAME, "storefront-request-1001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> contextRequestId = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> contextRequestId.set(MDC.get(RequestIdSupport.MDC_KEY)));

        assertThat(response.getHeader(RequestIdSupport.HEADER_NAME)).isEqualTo("storefront-request-1001");
        assertThat(contextRequestId.get()).isEqualTo("storefront-request-1001");
        assertThat(MDC.get(RequestIdSupport.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("위험하거나 없는 요청 ID는 안전한 ID로 교체한다")
    void replacesUnsafeRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader(RequestIdSupport.HEADER_NAME, "bad request id\nvalue");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(RequestIdSupport.HEADER_NAME)).matches("[a-f0-9]{32}");
    }
}
