package com.section.admin.auth.service;

import com.section.admin.auth.support.AdminPasswordEncoder;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapInitializerTest {

    private AdminUserRepository adminUserRepository;
    private AdminPasswordEncoder passwordEncoder;
    private AdminBootstrapInitializer initializer;

    @BeforeEach
    void setUp() {
        adminUserRepository = mock(AdminUserRepository.class);
        passwordEncoder = new AdminPasswordEncoder();
        initializer = new AdminBootstrapInitializer(adminUserRepository, passwordEncoder);
    }

    @Test
    @DisplayName("운영 DB가 비어 있으면 환경 설정으로 최고 관리자를 생성한다")
    void createsInitialSuperAdmin() throws Exception {
        configure(" ops.master ", "safe-password-1234", " 운영 총괄 ");
        when(adminUserRepository.count()).thenReturn(0L);

        initializer.run(new DefaultApplicationArguments());

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserRepository).saveAndFlush(captor.capture());
        AdminUser saved = captor.getValue();
        assertEquals("ops.master", saved.getLoginId());
        assertEquals("운영 총괄", saved.getName());
        assertEquals("ROLE_SUPER", saved.getRole());
        assertTrue(passwordEncoder.matches("safe-password-1234", saved.getPassword()));
    }

    @Test
    @DisplayName("기존 관리자가 있으면 부트스트랩 환경 설정을 사용하지 않는다")
    void skipsBootstrapWhenAdminExists() throws Exception {
        when(adminUserRepository.count()).thenReturn(1L);

        initializer.run(new DefaultApplicationArguments());

        verify(adminUserRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("빈 운영 DB에서 최초 관리자 비밀번호가 없으면 기동을 중단한다")
    void failsWhenBootstrapPasswordIsMissing() {
        configure("ops.master", "", "운영 총괄");
        when(adminUserRepository.count()).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> initializer.run(new DefaultApplicationArguments()));
    }

    private void configure(String loginId, String password, String name) {
        ReflectionTestUtils.setField(initializer, "loginId", loginId);
        ReflectionTestUtils.setField(initializer, "password", password);
        ReflectionTestUtils.setField(initializer, "name", name);
    }
}
