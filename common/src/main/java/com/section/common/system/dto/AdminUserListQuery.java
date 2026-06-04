package com.section.common.system.dto;

public record AdminUserListQuery(
        String keyword,
        String role,
        String status,
        Integer inactiveDays,
        Boolean neverLoggedInOnly
) {
}
