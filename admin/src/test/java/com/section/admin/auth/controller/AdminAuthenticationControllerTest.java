package com.section.admin.auth.controller;

import com.section.admin.auth.service.AdminAuthenticationService;
import com.section.admin.auth.service.AdminAuthenticationService.AuthenticatedAdmin;
import com.section.admin.auth.support.AdminLoginAttemptGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ConcurrentModel;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminAuthenticationControllerTest {

    private AdminAuthenticationService authenticationService;
    private AdminLoginAttemptGuard loginAttemptGuard;
    private AdminAuthenticationController controller;

    @BeforeEach
    void setUp() {
        authenticationService = mock(AdminAuthenticationService.class);
        loginAttemptGuard = mock(AdminLoginAttemptGuard.class);
        controller = new AdminAuthenticationController(authenticationService, loginAttemptGuard);
    }

    @Test
    @DisplayName("로그인 성공 시 세션에 관리자 식별 정보를 저장한다")
    void loginStoresAuthenticatedAdminInSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setMaxInactiveInterval(47 * 60);
        when(authenticationService.authenticate("master", "admin1234"))
                .thenReturn(Optional.of(new AuthenticatedAdmin(1L, "운영 총괄", "ROLE_SUPER")));

        String view = controller.login("master", "admin1234", request, new ConcurrentModel());

        assertEquals("redirect:/admin/dashboard", view);
        assertEquals(1L, request.getSession().getAttribute(AdminAuthenticationController.ADMIN_NO));
        assertEquals("운영 총괄", request.getSession().getAttribute(AdminAuthenticationController.ADMIN_NAME));
        assertEquals(47 * 60, request.getSession().getMaxInactiveInterval());
        verify(loginAttemptGuard).clear(request.getRemoteAddr(), "master");
    }

    @Test
    @DisplayName("로그인 실패 시 세션을 만들지 않고 오류를 표시한다")
    void loginFailureDoesNotCreateSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ConcurrentModel model = new ConcurrentModel();
        when(authenticationService.authenticate("unknown", "wrong-password")).thenReturn(Optional.empty());

        String view = controller.login("unknown", "wrong-password", request, model);

        assertEquals("views/login", view);
        assertEquals("아이디 또는 비밀번호를 확인해 주세요.", model.getAttribute("loginError"));
        assertNull(request.getSession(false));
        verify(loginAttemptGuard).recordFailure(request.getRemoteAddr(), "unknown");
    }

    @Test
    @DisplayName("잠긴 계정과 IP 조합은 인증 조회 전에 차단한다")
    void blockedAttemptDoesNotAuthenticate() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ConcurrentModel model = new ConcurrentModel();
        when(loginAttemptGuard.isBlocked(request.getRemoteAddr(), "master")).thenReturn(true);

        String view = controller.login("master", "admin1234", request, model);

        assertEquals("views/login", view);
        assertEquals("로그인 시도가 많습니다. 잠시 후 다시 시도해 주세요.", model.getAttribute("loginError"));
        verifyNoInteractions(authenticationService);
    }

    @Test
    @DisplayName("로그아웃은 세션을 무효화하고 브라우저 저장 데이터 제거를 지시한다")
    void logoutInvalidatesSessionAndClearsBrowserData() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(AdminAuthenticationController.ADMIN_NO, 1L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        String view = controller.logout(request, response);

        assertEquals("redirect:/admin/login", view);
        assertNull(request.getSession(false));
        assertEquals("\"cache\", \"cookies\", \"storage\"", response.getHeader("Clear-Site-Data"));
    }
}
