package com.section.admin.auth.config;

import com.section.admin.auth.controller.AdminAuthenticationController;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminAuthenticationInterceptor implements HandlerInterceptor {

    private static final String ACTIVE = "ACTIVE";
    private static final String ROLE_SUPER = "ROLE_SUPER";

    private final AdminUserRepository adminUserRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        HttpSession session = request.getSession(false);
        Optional<Long> adminNo = resolveAdminNo(session);
        Optional<AdminUser> activeAdmin = adminNo.flatMap(adminUserRepository::findById)
                .filter(admin -> ACTIVE.equals(admin.getStatus()));

        if (activeAdmin.isEmpty()) {
            if (session != null) {
                session.invalidate();
            }
            reject(request, response);
            return false;
        }

        AdminUser admin = activeAdmin.get();
        if (requiresSuperAdmin(request.getRequestURI()) && !ROLE_SUPER.equals(admin.getRole())) {
            rejectForbidden(request, response);
            return false;
        }
        session.setAttribute(AdminAuthenticationController.ADMIN_NAME, admin.getName());
        session.setAttribute(AdminAuthenticationController.ADMIN_ROLE, admin.getRole());
        return true;
    }

    private Optional<Long> resolveAdminNo(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(AdminAuthenticationController.ADMIN_NO);
        if (value instanceof Long adminNo) {
            return Optional.of(adminNo);
        }
        return Optional.empty();
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getRequestURI().startsWith("/api/admin/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"A003\",\"message\":\"관리자 로그인이 필요합니다.\",\"status\":401}");
            return;
        }
        response.sendRedirect("/admin/login");
    }

    private boolean requiresSuperAdmin(String requestUri) {
        return requestUri.equals("/admin/settings")
                || requestUri.startsWith("/admin/settings/logs")
                || requestUri.startsWith("/api/admin/users")
                || requestUri.startsWith("/api/admin/logs")
                || requestUri.startsWith("/api/admin/settings/system");
    }

    private void rejectForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getRequestURI().startsWith("/api/admin/")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"A004\",\"message\":\"최고 관리자 권한이 필요합니다.\",\"status\":403}");
            return;
        }
        response.sendRedirect("/admin/forbidden");
    }
}
