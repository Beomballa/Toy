package com.section.admin.common.config;

import com.section.common.system.support.AdminRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AdminAuditRequestFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Admin-No";
    private static final String SESSION_ATTRIBUTE_NAME = "ADMIN_NO";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            resolveAdminNo(request).ifPresent(AdminRequestContext::setCurrentAdminNo);
            filterChain.doFilter(request, response);
        } finally {
            AdminRequestContext.clear();
        }
    }

    private java.util.Optional<Long> resolveAdminNo(HttpServletRequest request) {
        String headerValue = request.getHeader(HEADER_NAME);
        if (headerValue != null && !headerValue.isBlank()) {
            try {
                return java.util.Optional.of(Long.parseLong(headerValue.trim()));
            } catch (NumberFormatException ignored) {
                return java.util.Optional.empty();
            }
        }

        Object sessionValue = request.getSession(false) != null
                ? request.getSession(false).getAttribute(SESSION_ATTRIBUTE_NAME)
                : null;

        if (sessionValue instanceof Long adminNo) {
            return java.util.Optional.of(adminNo);
        }

        if (sessionValue instanceof String adminNoText && !adminNoText.isBlank()) {
            try {
                return java.util.Optional.of(Long.parseLong(adminNoText.trim()));
            } catch (NumberFormatException ignored) {
                return java.util.Optional.empty();
            }
        }

        return java.util.Optional.empty();
    }
}
