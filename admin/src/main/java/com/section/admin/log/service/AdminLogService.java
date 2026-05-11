package com.section.admin.log.service;

import com.section.admin.log.req.AdminLogListRequest;
import com.section.admin.log.res.AdminLogDetailResponse;
import com.section.admin.log.res.AdminLogListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminActivityLogListQuery;
import com.section.common.system.dto.AdminActivityLogListResDto;
import com.section.common.system.entity.AdminActivityLog;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminActivityLogRepository;
import com.section.common.system.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLogService {

    private final AdminActivityLogRepository logRepository;
    private final AdminUserRepository adminUserRepository;

    @Transactional
    public void recordLog(Long adminNo, String actionType, Long targetId, String ipAddress) {
        AdminActivityLog log = AdminActivityLog.builder()
                .adminNo(adminNo)
                .actionType(actionType)
                .targetId(targetId)
                .ipAddress(ipAddress)
                .build();
        logRepository.save(log);
    }

    public Page<AdminActivityLog> getLogList(Pageable pageable) {
        return logRepository.findAllByOrderByActionDtmDesc(pageable);
    }

    public AdminLogListResponse getLogList(AdminLogListRequest req, Pageable pageable) {
        AdminActivityLogListQuery query = req.toQuery();
        Page<AdminActivityLogListResDto> page = logRepository.getLogList(query, pageable);
        Map<Long, String> adminNameMap = adminUserRepository.findAllById(
                page.getContent().stream().map(AdminActivityLogListResDto::getAdminNo).distinct().toList()
        ).stream().collect(Collectors.toMap(AdminUser::getAdminNo, AdminUser::getName));
        return AdminLogListResponse.of(page, query, adminNameMap);
    }

    public AdminLogDetailResponse getLogDetail(Long logNo) {
        AdminActivityLog log = logRepository.findById(logNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        String adminName = adminUserRepository.findById(log.getAdminNo())
                .map(AdminUser::getName)
                .orElse("관리자#" + log.getAdminNo());
        return AdminLogDetailResponse.from(log, adminName);
    }
}
