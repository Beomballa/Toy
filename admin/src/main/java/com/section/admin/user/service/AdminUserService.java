package com.section.admin.user.service;

import com.section.admin.user.req.AdminUserSaveRequest;
import com.section.admin.user.res.AdminUserListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;

    public List<AdminUserListResponse> getAdminList() {
        return adminUserRepository.findAll().stream()
                .map(AdminUserListResponse::from)
                .toList();
    }

    @Transactional
    public void saveAdmin(AdminUserSaveRequest req) {
        if (req.isNewAdmin()) {
            adminUserRepository.findByLoginId(req.loginId()).ifPresent(u -> {
                throw new BusinessException("이미 사용중인 ID입니다.", ErrorCode.INVALID_INPUT_VALUE);
            });

            if (req.password() == null || req.password().isBlank()) {
                throw new BusinessException("비밀번호는 필수입니다.", ErrorCode.INVALID_INPUT_VALUE);
            }

            AdminUser adminUser = AdminUser.builder()
                    .loginId(req.loginId())
                    .password(req.password())
                    .name(req.name())
                    .role(req.role())
                    .status(req.status())
                    .build();
            adminUserRepository.save(adminUser);
            return;
        }

        AdminUser adminUser = adminUserRepository.findById(req.adminNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        adminUser.updateInfo(req.name(), req.role(), req.status());

        if (req.password() != null && !req.password().isBlank()) {
            adminUser.changePassword(req.password());
        }
    }

    @Transactional
    public void deleteAdmin(Long adminNo) {
        AdminUser adminUser = adminUserRepository.findById(adminNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        adminUserRepository.delete(adminUser);
    }
}
