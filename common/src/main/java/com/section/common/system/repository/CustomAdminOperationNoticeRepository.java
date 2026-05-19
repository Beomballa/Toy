package com.section.common.system.repository;

import com.section.common.system.dto.AdminOperationNoticeListQuery;
import com.section.common.system.dto.AdminOperationNoticeListResDto;
import com.section.common.system.entity.AdminOperationNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomAdminOperationNoticeRepository {

    Page<AdminOperationNoticeListResDto> getNoticeList(AdminOperationNoticeListQuery query, Pageable pageable);

    List<AdminOperationNotice> getActiveDashboardNotices(LocalDateTime now, int limit);
}
