package com.section.admin.user.res;

import com.section.common.system.entity.AdminUser;

import java.time.LocalDateTime;

public record AdminUserListResponse(
        Long adminNo,
        String loginId,
        String name,
        String role,
        String status,
        LocalDateTime lastLoginDtm,
        LocalDateTime crtDtm
) {
    public static AdminUserListResponse from(AdminUser adminUser) {
        return new AdminUserListResponse(
                adminUser.getAdminNo(),
                adminUser.getLoginId(),
                adminUser.getName(),
                adminUser.getRole(),
                adminUser.getStatus(),
                adminUser.getLastLoginDtm(),
                adminUser.getCrtDtm()
        );
    }
}
