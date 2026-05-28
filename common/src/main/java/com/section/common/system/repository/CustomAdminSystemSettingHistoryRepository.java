package com.section.common.system.repository;

import com.section.common.system.dto.AdminSystemSettingHistoryListQuery;
import com.section.common.system.dto.AdminSystemSettingHistoryListResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomAdminSystemSettingHistoryRepository {

    Page<AdminSystemSettingHistoryListResDto> getHistoryList(AdminSystemSettingHistoryListQuery query, Pageable pageable);
}
