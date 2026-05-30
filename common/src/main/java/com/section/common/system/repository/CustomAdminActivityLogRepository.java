package com.section.common.system.repository;

import com.section.common.system.dto.AdminActivityLogListQuery;
import com.section.common.system.dto.AdminActivityLogListResDto;
import com.section.common.system.dto.AdminActivityLogSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomAdminActivityLogRepository {

    Page<AdminActivityLogListResDto> getLogList(AdminActivityLogListQuery query, Pageable pageable);

    AdminActivityLogSummaryDto getLogSummary(AdminActivityLogListQuery query);
}
