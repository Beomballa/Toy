package com.section.admin.system.config;

import com.section.common.system.support.RequestIdSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class BatchRequestIdFilterTest {

    @Test
    @DisplayName("배치 헬스 요청에 추적 ID를 생성하고 로그 컨텍스트를 정리한다")
    void addsRequestIdAndClearsLoggingContext() throws Exception {
        BatchRequestIdFilter filter = new BatchRequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health/ready");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> contextRequestId = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> contextRequestId.set(MDC.get(RequestIdSupport.MDC_KEY)));

        assertThat(response.getHeader(RequestIdSupport.HEADER_NAME)).matches("[a-f0-9]{32}");
        assertThat(contextRequestId.get()).isEqualTo(response.getHeader(RequestIdSupport.HEADER_NAME));
        assertThat(MDC.get(RequestIdSupport.MDC_KEY)).isNull();
    }
}
