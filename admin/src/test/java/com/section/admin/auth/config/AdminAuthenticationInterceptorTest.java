package com.section.admin.auth.config;

import com.section.admin.auth.controller.AdminAuthenticationController;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminAuthenticationInterceptorTest {

    private AdminUserRepository adminUserRepository;
    private AdminAuthenticationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        adminUserRepository = mock(AdminUserRepository.class);
        interceptor = new AdminAuthenticationInterceptor(adminUserRepository);
    }

    @Test
    @DisplayName("비로그인 관리자 화면 요청은 로그인 화면으로 이동한다")
    void redirectsUnauthenticatedPageRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals("/admin/login", response.getRedirectedUrl());
    }

    @Test
    @DisplayName("비로그인 관리자 API 요청은 401 JSON을 반환한다")
    void rejectsUnauthenticatedApiRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard/stats");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("A003"));
    }

    @Test
    @DisplayName("활성 관리자 세션은 요청을 통과한다")
    void allowsActiveAdminSession() throws Exception {
        AdminUser admin = AdminUser.builder()
                .adminNo(3L).loginId("ops").password("encoded")
                .name("운영자").role("ROLE_ADMIN").status("ACTIVE").build();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/dashboard");
        request.getSession().setAttribute(AdminAuthenticationController.ADMIN_NO, 3L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(adminUserRepository.findById(3L)).thenReturn(Optional.of(admin));

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals("운영자", request.getSession().getAttribute(AdminAuthenticationController.ADMIN_NAME));
    }

    @Test
    @DisplayName("정지된 관리자 세션은 즉시 폐기한다")
    void invalidatesSuspendedAdminSession() throws Exception {
        AdminUser admin = AdminUser.builder()
                .adminNo(4L).loginId("blocked").password("encoded")
                .name("정지 계정").role("ROLE_ADMIN").status("SUSPENDED").build();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/dashboard");
        var session = request.getSession();
        session.setAttribute(AdminAuthenticationController.ADMIN_NO, 4L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(adminUserRepository.findById(4L)).thenReturn(Optional.of(admin));

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertThrows(IllegalStateException.class, () -> session.getAttribute(AdminAuthenticationController.ADMIN_NO));
    }
}
