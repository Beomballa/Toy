package com.section.admin.auth.service;

import com.section.admin.auth.support.AdminPasswordEncoder;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class AdminBootstrapInitializer implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final AdminPasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.login-id:}")
    private String loginId;

    @Value("${admin.bootstrap.password:}")
    private String password;

    @Value("${admin.bootstrap.name:Initial Administrator}")
    private String name;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminUserRepository.count() > 0) {
            return;
        }

        String normalizedLoginId = normalize(loginId);
        String normalizedPassword = password == null ? "" : password;
        String normalizedName = normalize(name);
        validate(normalizedLoginId, normalizedPassword, normalizedName);

        adminUserRepository.saveAndFlush(AdminUser.builder()
                .loginId(normalizedLoginId)
                .password(passwordEncoder.encode(normalizedPassword))
                .name(normalizedName)
                .role("ROLE_SUPER")
                .status("ACTIVE")
                .build());
    }

    private void validate(String normalizedLoginId, String normalizedPassword, String normalizedName) {
        if (normalizedLoginId.isBlank() || normalizedLoginId.length() > 50) {
            throw new IllegalStateException("운영 최초 관리자 ID를 ADMIN_BOOTSTRAP_LOGIN_ID로 설정해야 합니다.");
        }
        if (normalizedPassword.length() < 12 || normalizedPassword.length() > 100) {
            throw new IllegalStateException("운영 최초 관리자 비밀번호는 ADMIN_BOOTSTRAP_PASSWORD에 12자 이상으로 설정해야 합니다.");
        }
        if (normalizedName.isBlank() || normalizedName.length() > 50) {
            throw new IllegalStateException("운영 최초 관리자 이름을 ADMIN_BOOTSTRAP_NAME으로 설정해야 합니다.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
