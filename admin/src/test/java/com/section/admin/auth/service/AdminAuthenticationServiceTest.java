package com.section.admin.auth.service;

import com.section.admin.auth.support.AdminPasswordEncoder;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthenticationServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    private AdminPasswordEncoder passwordEncoder;
    private AdminAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new AdminPasswordEncoder();
        authenticationService = new AdminAuthenticationService(adminUserRepository, passwordEncoder);
    }

    @Test
    @DisplayName("활성 관리자는 로그인하고 기존 평문 비밀번호를 해시로 전환한다")
    void authenticateMigratesLegacyPassword() {
        AdminUser admin = AdminUser.builder()
                .adminNo(1L).loginId("master").password("admin1234")
                .name("운영자").role("ROLE_SUPER").status("ACTIVE").build();
        when(adminUserRepository.findByLoginIdIgnoreCase("master")).thenReturn(Optional.of(admin));

        assertTrue(authenticationService.authenticate(" master ", "admin1234").isPresent());
        assertTrue(passwordEncoder.isEncoded(admin.getPassword()));
        assertNotNull(admin.getLastLoginDtm());
    }

    @Test
    @DisplayName("정지 관리자는 올바른 비밀번호로도 로그인할 수 없다")
    void authenticateRejectsSuspendedAdmin() {
        AdminUser admin = AdminUser.builder()
                .adminNo(2L).loginId("suspended").password("admin1234")
                .name("정지 계정").role("ROLE_ADMIN").status("SUSPENDED").build();
        when(adminUserRepository.findByLoginIdIgnoreCase("suspended")).thenReturn(Optional.of(admin));

        assertFalse(authenticationService.authenticate("suspended", "admin1234").isPresent());
    }

    @Test
    @DisplayName("존재하지 않는 계정은 인증되지 않는다")
    void authenticateRejectsUnknownAdmin() {
        when(adminUserRepository.findByLoginIdIgnoreCase("unknown")).thenReturn(Optional.empty());

        assertFalse(authenticationService.authenticate("unknown", "password1234").isPresent());
        verify(adminUserRepository).findByLoginIdIgnoreCase("unknown");
    }

    @Test
    @DisplayName("낮은 비용의 유효 PBKDF2 비밀번호는 로그인 후 현재 기준으로 재해시한다")
    void authenticateRehashesLowerCostPassword() throws Exception {
        String lowerCostPassword = encodeWithIterations("admin1234", 100_000);
        AdminUser admin = AdminUser.builder()
                .adminNo(3L).loginId("legacy-hash").password(lowerCostPassword)
                .name("운영자").role("ROLE_ADMIN").status("ACTIVE").build();
        when(adminUserRepository.findByLoginIdIgnoreCase("legacy-hash")).thenReturn(Optional.of(admin));

        assertTrue(authenticationService.authenticate("legacy-hash", "admin1234").isPresent());
        assertTrue(passwordEncoder.isEncoded(admin.getPassword()));
        assertFalse(passwordEncoder.needsRehash(admin.getPassword()));
    }

    private String encodeWithIterations(String password, int iterations) throws Exception {
        byte[] salt = new byte[16];
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return "{pbkdf2}" + iterations + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } finally {
            spec.clearPassword();
        }
    }
}
