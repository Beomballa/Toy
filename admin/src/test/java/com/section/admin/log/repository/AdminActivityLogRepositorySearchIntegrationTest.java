package com.section.admin.log.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.system.dto.AdminActivityLogListQuery;
import com.section.common.system.entity.AdminActivityLog;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminActivityLogRepository;
import com.section.common.system.repository.AdminUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class AdminActivityLogRepositorySearchIntegrationTest {

    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Test
    @DisplayName("활동 로그 검색은 관리자명 키워드로 다중 토큰 검색을 지원한다")
    void getLogListMatchesAdminKeywordTokens() {
        AdminUser matchedAdmin = adminUserRepository.save(AdminUser.builder()
                .loginId("ops-history")
                .password("pw")
                .name("정산 운영자")
                .build());
        AdminUser otherAdmin = adminUserRepository.save(AdminUser.builder()
                .loginId("ops-other")
                .password("pw")
                .name("일반 관리자")
                .build());

        adminActivityLogRepository.save(log(matchedAdmin.getAdminNo(), "TASK_UPDATE", 11L, LocalDateTime.of(2026, 6, 14, 9, 0)));
        adminActivityLogRepository.save(log(otherAdmin.getAdminNo(), "TASK_UPDATE", 12L, LocalDateTime.of(2026, 6, 14, 9, 10)));

        var page = adminActivityLogRepository.getLogList(
                new AdminActivityLogListQuery(null, "정산 운영자", "TASK_", null, null, null),
                PageRequest.of(0, 10)
        );

        assertEquals(1, page.getTotalElements());
        assertEquals(matchedAdmin.getAdminNo(), page.getContent().getFirst().getAdminNo());
    }

    private AdminActivityLog log(Long adminNo, String actionType, Long targetId, LocalDateTime actionDtm) {
        AdminActivityLog log = AdminActivityLog.builder()
                .adminNo(adminNo)
                .actionType(actionType)
                .targetId(targetId)
                .ipAddress("127.0.0.1")
                .build();
        setField(log, "actionDtm", actionDtm);
        return log;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}
