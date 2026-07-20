package com.section.admin.auth.controller;

import com.section.admin.auth.service.AdminAuthenticationService;
import com.section.admin.auth.service.AdminAuthenticationService.AuthenticatedAdmin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AdminAuthenticationController {

    public static final String ADMIN_NO = "ADMIN_NO";
    public static final String ADMIN_NAME = "ADMIN_NAME";
    public static final String ADMIN_ROLE = "ADMIN_ROLE";

    private final AdminAuthenticationService authenticationService;

    @GetMapping("/")
    public String root() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/login")
    public String loginPage(HttpServletRequest request) {
        return request.getSession(false) != null && request.getSession(false).getAttribute(ADMIN_NO) != null
                ? "redirect:/admin/dashboard"
                : "views/login";
    }

    @PostMapping("/admin/login")
    public String login(
            @RequestParam String loginId,
            @RequestParam String password,
            HttpServletRequest request,
            Model model
    ) {
        Optional<AuthenticatedAdmin> authenticated = authenticationService.authenticate(loginId, password);
        if (authenticated.isEmpty()) {
            model.addAttribute("loginError", "아이디 또는 비밀번호를 확인해 주세요.");
            model.addAttribute("loginId", loginId == null ? "" : loginId.trim());
            return "views/login";
        }

        HttpSession session = request.getSession(true);
        request.changeSessionId();
        AuthenticatedAdmin admin = authenticated.get();
        session.setAttribute(ADMIN_NO, admin.adminNo());
        session.setAttribute(ADMIN_NAME, admin.name());
        session.setAttribute(ADMIN_ROLE, admin.role());
        session.setMaxInactiveInterval(30 * 60);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/admin/login";
    }
}
