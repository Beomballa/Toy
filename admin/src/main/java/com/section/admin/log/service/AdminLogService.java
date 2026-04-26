package com.section.admin.log.service;

import com.section.common.system.entity.AdminActivityLog;
import com.section.common.system.repository.AdminActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLogService {

    private final AdminActivityLogRepository logRepository;

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
}
