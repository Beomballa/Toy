package com.section.admin.common.config;

import com.section.common.system.support.AdminRequestContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AdminAuditRequestFilterTest {

    private final AdminAuditRequestFilter filter = new AdminAuditRequestFilter();

    @AfterEach
    void tearDown() {
        AdminRequestContext.clear();
    }

    @Test
    @DisplayName("감사 필터는 헤더의 관리자 번호를 요청 컨텍스트에 주입한다")
    void filterReadsAdminNoFromHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Admin-No", "7");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertEquals(7L, AdminRequestContext.getCurrentAdminNo().orElseThrow())
        );

        assertFalse(AdminRequestContext.getCurrentAdminNo().isPresent());
    }

    @Test
    @DisplayName("감사 필터는 세션의 관리자 번호도 요청 컨텍스트에 주입한다")
    void filterReadsAdminNoFromSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("ADMIN_NO", "9");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertEquals(9L, AdminRequestContext.getCurrentAdminNo().orElseThrow())
        );
    }
}
