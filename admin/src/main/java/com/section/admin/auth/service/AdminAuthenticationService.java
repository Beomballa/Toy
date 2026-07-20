package com.section.admin.auth.service;

import com.section.admin.auth.support.AdminPasswordEncoder;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminAuthenticationService {

    private static final String ACTIVE = "ACTIVE";

    private final AdminUserRepository adminUserRepository;
    private final AdminPasswordEncoder passwordEncoder;

    @Transactional
    public Optional<AuthenticatedAdmin> authenticate(String loginId, String password) {
        String normalizedLoginId = loginId == null ? "" : loginId.trim();
        String rawPassword = password == null ? "" : password;
        if (normalizedLoginId.length() > 50 || rawPassword.length() > 100) {
            passwordEncoder.consumeDummyMatch(rawPassword.substring(0, Math.min(rawPassword.length(), 100)));
            return Optional.empty();
        }
        Optional<AdminUser> candidate = adminUserRepository.findByLoginIdIgnoreCase(normalizedLoginId);

        if (candidate.isEmpty()) {
            // 존재하지 않는 계정도 해시 연산을 거쳐 계정 유무에 따른 응답 시간 차이를 줄인다.
            passwordEncoder.consumeDummyMatch(rawPassword);
            return Optional.empty();
        }

        AdminUser admin = candidate.get();
        boolean passwordMatches = passwordEncoder.matches(rawPassword, admin.getPassword());
        if (!ACTIVE.equals(admin.getStatus()) || !passwordMatches) {
            return Optional.empty();
        }

        if (!passwordEncoder.isEncoded(admin.getPassword())) {
            admin.changePassword(passwordEncoder.encode(rawPassword));
        }
        admin.recordLogin(LocalDateTime.now());
        return Optional.of(new AuthenticatedAdmin(admin.getAdminNo(), admin.getName(), admin.getRole()));
    }

    public record AuthenticatedAdmin(Long adminNo, String name, String role) {
    }
}
