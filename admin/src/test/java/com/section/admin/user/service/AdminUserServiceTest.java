package com.section.admin.user.service;

import com.section.admin.user.req.AdminUserListRequest;
import com.section.admin.user.req.AdminUserSaveRequest;
import com.section.admin.user.res.AdminUserListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminUserListQuery;
import com.section.common.system.dto.AdminUserListResDto;
import com.section.common.system.dto.AdminUserSummaryDto;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import com.section.common.system.support.AdminRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @AfterEach
    void tearDown() {
        AdminRequestContext.clear();
    }

    @Test
    @DisplayName("관리자 목록은 검색 조건과 페이지를 반영한 응답을 반환한다")
    void getAdminListReturnsPagedResponse() {
        AdminUserListRequest request = new AdminUserListRequest();
        request.setKeyword("master");
        AdminUserListResDto row = new AdminUserListResDto();
        row.setAdminNo(1L);
        row.setLoginId("master");
        row.setName("최고관리자");
        row.setRole("ROLE_SUPER");
        row.setStatus("ACTIVE");
        row.setLastLoginDtm(LocalDateTime.of(2026, 6, 4, 9, 0));
        row.setCrtDtm(LocalDateTime.of(2026, 5, 1, 8, 0));

        when(adminUserRepository.getAdminUserList(
                new AdminUserListQuery("master", null, null, null, null),
                PageRequest.of(0, 10)
        )).thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1));
        when(adminUserRepository.getAdminUserSummary(any(), any()))
                .thenReturn(new AdminUserSummaryDto(1, 1, 0, 1, 0));

        AdminUserListResponse response = adminUserService.getAdminList(request, null, null);

        assertEquals(1, response.items().size());
        assertEquals("최고 관리자", response.items().getFirst().roleLabel());
        assertEquals("검색 결과 1명", response.resultMeta().resultLabel());
    }

    @Test
    @DisplayName("관리자 목록 CSV 내보내기는 필터 결과를 파일 바이트로 만든다")
    void exportAdminListCsvReturnsCsvBytes() {
        AdminUserListRequest request = new AdminUserListRequest();
        request.setStatus("ACTIVE");
        request.setNeverLoggedInOnly("Y");

        AdminUserListResDto row = new AdminUserListResDto();
        row.setAdminNo(3L);
        row.setLoginId("ops.master");
        row.setName("운영 총괄");
        row.setRole("ROLE_SUPER");
        row.setStatus("ACTIVE");
        row.setLastLoginDtm(null);
        row.setCrtDtm(LocalDateTime.of(2026, 6, 1, 10, 0));

        when(adminUserRepository.getAdminUserList(
                new AdminUserListQuery(null, null, "ACTIVE", null, true),
                PageRequest.of(0, 1000)
        )).thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 1000), 1));
        when(adminUserRepository.getAdminUserSummary(any(), any()))
                .thenReturn(new AdminUserSummaryDto(1, 1, 0, 1, 1));

        String csv = new String(adminUserService.exportAdminListCsv(request), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"조회조건\",\"권한 우선 · 최근 로그인순 · 상태=활성 · 로그인 이력 없음\""));
        assertTrue(csv.contains("\"ops.master\""));
        assertTrue(csv.contains("\"-\""));
    }

    @Test
    @DisplayName("관리자 저장은 입력값을 정규화해 신규 계정을 만든다")
    void saveAdminNormalizesFieldsWhenCreating() {
        adminUserService.saveAdmin(new AdminUserSaveRequest(
                null,
                " master ",
                "  pass1234  ",
                " 운영 총괄 ",
                " role_super ",
                " active "
        ));

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserRepository).save(captor.capture());
        assertEquals("master", captor.getValue().getLoginId());
        assertEquals("pass1234", captor.getValue().getPassword());
        assertEquals("운영 총괄", captor.getValue().getName());
        assertEquals("ROLE_SUPER", captor.getValue().getRole());
        assertEquals("ACTIVE", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("관리자 저장은 로그인 ID를 대소문자 구분 없이 중복 검사한다")
    void saveAdminRejectsDuplicateLoginIdIgnoringCase() {
        when(adminUserRepository.existsByLoginIdIgnoreCase("Master")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> adminUserService.saveAdmin(
                new AdminUserSaveRequest(null, " Master ", "pass1234", "운영 총괄", "ROLE_SUPER", "ACTIVE")
        ));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        verify(adminUserRepository, never()).save(any(AdminUser.class));
    }

    @Test
    @DisplayName("마지막 활성 최고관리자는 일반 관리자나 정지 상태로 변경할 수 없다")
    void saveAdminRejectsDemotingLastActiveSuperAdmin() {
        AdminUser adminUser = AdminUser.builder()
                .adminNo(1L)
                .loginId("master")
                .password("pw")
                .name("최고관리자")
                .role("ROLE_SUPER")
                .status("ACTIVE")
                .build();
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(adminUserRepository.countByRoleAndStatus("ROLE_SUPER", "ACTIVE")).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> adminUserService.saveAdmin(
                new AdminUserSaveRequest(1L, "master", null, "최고관리자", "ROLE_ADMIN", "SUSPENDED")
        ));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("현재 로그인한 관리자 계정은 삭제할 수 없다")
    void deleteAdminRejectsCurrentAdmin() {
        AdminRequestContext.setCurrentAdminNo(7L);
        AdminUser adminUser = AdminUser.builder()
                .adminNo(7L)
                .loginId("manager")
                .password("pw")
                .name("운영자")
                .role("ROLE_ADMIN")
                .status("ACTIVE")
                .build();
        when(adminUserRepository.findById(7L)).thenReturn(Optional.of(adminUser));

        BusinessException exception = assertThrows(BusinessException.class, () -> adminUserService.deleteAdmin(7L));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("관리자 삭제는 존재하는 관리자 엔티티를 조회 후 삭제한다")
    void deleteAdminDeletesExistingEntity() {
        AdminUser adminUser = AdminUser.builder()
                .adminNo(8L)
                .loginId("manager")
                .password("pw")
                .name("운영자")
                .role("ROLE_ADMIN")
                .status("ACTIVE")
                .build();
        when(adminUserRepository.findById(8L)).thenReturn(Optional.of(adminUser));

        adminUserService.deleteAdmin(8L);

        verify(adminUserRepository).delete(argThat(item -> item.getAdminNo().equals(8L)));
    }
}
