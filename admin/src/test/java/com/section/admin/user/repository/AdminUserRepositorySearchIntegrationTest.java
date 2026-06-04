package com.section.admin.user.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.system.dto.AdminUserListQuery;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class AdminUserRepositorySearchIntegrationTest {

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Test
    @DisplayName("관리자 목록 검색은 공백 단위 다중 키워드를 모두 만족하는 계정만 찾는다")
    void getAdminUserListMatchesAllKeywordTokens() {
        adminUserRepository.save(AdminUser.builder()
                .loginId("master.ops")
                .password("pw")
                .name("마스터 운영")
                .role("ROLE_SUPER")
                .status("ACTIVE")
                .lastLoginDtm(LocalDateTime.now().minusDays(1))
                .build());
        adminUserRepository.save(AdminUser.builder()
                .loginId("master.finance")
                .password("pw")
                .name("마스터 정산")
                .role("ROLE_SUPER")
                .status("ACTIVE")
                .lastLoginDtm(LocalDateTime.now().minusDays(1))
                .build());

        Page<?> result = adminUserRepository.getAdminUserList(
                new AdminUserListQuery("master 운영", null, null, null, null),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("관리자 요약은 최근 7일 미접속 계정 수를 함께 집계한다")
    void getAdminUserSummaryIncludesInactiveCount() {
        adminUserRepository.save(AdminUser.builder()
                .loginId("inactive.admin")
                .password("pw")
                .name("휴면 관리자")
                .role("ROLE_ADMIN")
                .status("ACTIVE")
                .lastLoginDtm(LocalDateTime.now().minusDays(10))
                .build());
        adminUserRepository.save(AdminUser.builder()
                .loginId("fresh.admin")
                .password("pw")
                .name("최근 관리자")
                .role("ROLE_ADMIN")
                .status("ACTIVE")
                .lastLoginDtm(LocalDateTime.now().minusDays(1))
                .build());

        var summary = adminUserRepository.getAdminUserSummary(
                new AdminUserListQuery(null, null, null, null, null),
                LocalDateTime.now()
        );

        assertEquals(1L, summary.inactiveCount());
    }

    @Test
    @DisplayName("관리자 목록 검색은 로그인 이력이 없는 계정만 별도로 필터링할 수 있다")
    void getAdminUserListFiltersNeverLoggedInUsers() {
        adminUserRepository.save(AdminUser.builder()
                .loginId("never.login")
                .password("pw")
                .name("미로그인 관리자")
                .role("ROLE_ADMIN")
                .status("ACTIVE")
                .lastLoginDtm(null)
                .build());
        adminUserRepository.save(AdminUser.builder()
                .loginId("logged.in")
                .password("pw")
                .name("로그인 관리자")
                .role("ROLE_ADMIN")
                .status("ACTIVE")
                .lastLoginDtm(LocalDateTime.now().minusDays(2))
                .build());

        Page<?> result = adminUserRepository.getAdminUserList(
                new AdminUserListQuery(null, null, null, null, true),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
    }
}
