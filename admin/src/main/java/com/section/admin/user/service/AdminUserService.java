package com.section.admin.user.service;

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

    public List<AdminUser> getAdminList() {
        return adminUserRepository.findAll();
    }

    @Transactional
    public void saveAdmin(AdminUser adminUser) {
        if (adminUser.getAdminNo() == null) {
            // 신규 등록 시 중복 체크
            adminUserRepository.findByLoginId(adminUser.getLoginId()).ifPresent(u -> {
                throw new BusinessException("이미 사용중인 ID입니다.", ErrorCode.INVALID_INPUT_VALUE);
            });
        }
        adminUserRepository.save(adminUser);
    }

    @Transactional
    public void deleteAdmin(Long adminNo) {
        adminUserRepository.deleteById(adminNo);
    }
}
