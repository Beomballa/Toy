package com.section.admin.user.service;

import com.section.admin.user.req.AdminUserListRequest;
import com.section.admin.user.req.AdminUserSaveRequest;
import com.section.admin.user.res.AdminUserListResponse;
import com.section.admin.user.support.AdminUserExportCsvWriter;
import com.section.admin.user.support.AdminUserExportSummary;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminUserListQuery;
import com.section.common.system.dto.AdminUserListResDto;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import com.section.common.system.support.AdminRequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {
    private static final int ADMIN_EXPORT_MAX_SIZE = 1000;
    private static final String ROLE_SUPER = "ROLE_SUPER";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUSPENDED = "SUSPENDED";

    private final AdminUserRepository adminUserRepository;

    public AdminUserListResponse getAdminList(AdminUserListRequest req, Integer page, Integer size) {
        AdminUserListQuery query = req.toQuery();
        PageRequest pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        Page<AdminUserListResDto> resultPage = adminUserRepository.getAdminUserList(query, pageable);
        return AdminUserListResponse.of(
                resultPage,
                query,
                adminUserRepository.getAdminUserSummary(query, LocalDateTime.now())
        );
    }

    public byte[] exportAdminListCsv(AdminUserListRequest req) {
        AdminUserListQuery query = req.toQuery();
        Page<AdminUserListResDto> resultPage = adminUserRepository.getAdminUserList(query, PageRequest.of(0, ADMIN_EXPORT_MAX_SIZE));
        return AdminUserExportCsvWriter.write(
                AdminUserExportSummary.of(query, LocalDateTime.now()),
                AdminUserListResponse.of(
                        resultPage,
                        query,
                        adminUserRepository.getAdminUserSummary(query, LocalDateTime.now())
                ).items()
        );
    }

    @Transactional
    public void saveAdmin(AdminUserSaveRequest req) {
        String normalizedLoginId = normalizeRequiredText(req.loginId());
        String normalizedName = normalizeRequiredText(req.name());
        String normalizedRole = normalizeRole(req.role());
        String normalizedStatus = normalizeStatus(req.status());

        if (req.isNewAdmin()) {
            if (adminUserRepository.existsByLoginIdIgnoreCase(normalizedLoginId)) {
                throw new BusinessException("이미 사용중인 ID입니다.", ErrorCode.INVALID_INPUT_VALUE);
            }

            String normalizedPassword = normalizePassword(req.password());
            if (normalizedPassword == null) {
                throw new BusinessException("비밀번호는 필수입니다.", ErrorCode.INVALID_INPUT_VALUE);
            }

            AdminUser adminUser = AdminUser.builder()
                    .loginId(normalizedLoginId)
                    .password(normalizedPassword)
                    .name(normalizedName)
                    .role(normalizedRole)
                    .status(normalizedStatus)
                    .build();
            adminUserRepository.save(adminUser);
            return;
        }

        AdminUser adminUser = adminUserRepository.findById(req.adminNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        assertSameLoginId(adminUser, normalizedLoginId);
        assertLastActiveSuperAdminEditable(adminUser, normalizedRole, normalizedStatus);
        adminUser.updateInfo(normalizedName, normalizedRole, normalizedStatus);

        String normalizedPassword = normalizePassword(req.password());
        if (normalizedPassword != null) {
            adminUser.changePassword(normalizedPassword);
        }
    }

    @Transactional
    public void deleteAdmin(Long adminNo) {
        AdminUser adminUser = adminUserRepository.findById(adminNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        if (AdminRequestContext.getCurrentAdminNo().filter(adminNo::equals).isPresent()) {
            throw new BusinessException("현재 로그인한 관리자 계정은 삭제할 수 없습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        assertRemainingActiveSuperAdmin(adminUser, ROLE_SUPER, STATUS_ACTIVE);
        adminUserRepository.delete(adminUser);
    }

    private void assertLastActiveSuperAdminEditable(AdminUser adminUser, String nextRole, String nextStatus) {
        assertRemainingActiveSuperAdmin(adminUser, nextRole, nextStatus);
    }

    private void assertSameLoginId(AdminUser adminUser, String requestedLoginId) {
        if (!adminUser.getLoginId().equals(requestedLoginId)) {
            throw new BusinessException("로그인 ID는 변경할 수 없습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void assertRemainingActiveSuperAdmin(AdminUser adminUser, String nextRole, String nextStatus) {
        boolean currentlyProtected = ROLE_SUPER.equals(adminUser.getRole()) && STATUS_ACTIVE.equals(adminUser.getStatus());
        boolean remainsProtected = ROLE_SUPER.equals(nextRole) && STATUS_ACTIVE.equals(nextStatus);

        if (currentlyProtected && !remainsProtected
                && adminUserRepository.countByRoleAndStatus(ROLE_SUPER, STATUS_ACTIVE) <= 1) {
            throw new BusinessException("최소 1명의 활성 최고관리자는 유지되어야 합니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return 10;
        }
        return Math.min(size, 50);
    }

    private String normalizeRequiredText(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    private String normalizePassword(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeRole(String value) {
        String normalized = normalizeRequiredText(value).toUpperCase();
        if (!ROLE_SUPER.equals(normalized) && !"ROLE_ADMIN".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    private String normalizeStatus(String value) {
        String normalized = normalizeRequiredText(value).toUpperCase();
        if (!STATUS_ACTIVE.equals(normalized) && !STATUS_SUSPENDED.equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }
}
