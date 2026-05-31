package com.section.admin.user.service;

import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    @DisplayName("관리자 삭제는 존재하는 관리자 엔티티를 조회 후 삭제한다")
    void deleteAdminDeletesExistingEntity() {
        AdminUser adminUser = AdminUser.builder()
                .adminNo(7L)
                .loginId("manager")
                .password("pw")
                .name("운영자")
                .role("ROLE_ADMIN")
                .status("ACTIVE")
                .build();
        when(adminUserRepository.findById(7L)).thenReturn(Optional.of(adminUser));

        adminUserService.deleteAdmin(7L);

        verify(adminUserRepository).delete(argThat(item -> item.getAdminNo().equals(7L)));
    }
}
