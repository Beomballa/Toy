package com.section.common.system.repository;

import com.section.common.system.dto.AdminUserListQuery;
import com.section.common.system.dto.AdminUserListResDto;
import com.section.common.system.dto.AdminUserSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface CustomAdminUserRepository {

    Page<AdminUserListResDto> getAdminUserList(AdminUserListQuery query, Pageable pageable);

    AdminUserSummaryDto getAdminUserSummary(AdminUserListQuery query, LocalDateTime now);
}
