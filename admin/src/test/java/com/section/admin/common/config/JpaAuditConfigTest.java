package com.section.admin.common.config;

import com.section.common.base.entity.type.config.JpaAuditConfig;
import com.section.common.system.support.AdminRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JpaAuditConfigTest {

    @AfterEach
    void tearDown() {
        AdminRequestContext.clear();
    }

    @Test
    @DisplayName("감사 제공자는 요청 컨텍스트의 관리자 번호를 우선 사용한다")
    void auditorProviderPrefersRequestContext() {
        AdminRequestContext.setCurrentAdminNo(15L);

        Long auditor = new JpaAuditConfig().auditorProvider().getCurrentAuditor().orElseThrow();

        assertEquals(15L, auditor);
    }

    @Test
    @DisplayName("감사 제공자는 요청 컨텍스트가 없으면 1번 관리자를 기본값으로 사용한다")
    void auditorProviderFallsBackToDefaultAdminNo() {
        Long auditor = new JpaAuditConfig().auditorProvider().getCurrentAuditor().orElseThrow();

        assertEquals(1L, auditor);
    }
}
