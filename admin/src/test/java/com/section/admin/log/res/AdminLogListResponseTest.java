package com.section.admin.log.res;

import com.section.common.system.dto.AdminActivityLogListQuery;
import com.section.common.system.dto.AdminActivityLogListResDto;
import com.section.common.system.dto.AdminActivityLogSummaryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminLogListResponseTest {

    @Test
    @DisplayName("활동 로그 목록 메타는 전체 페이지 수와 null 관리자명을 올바르게 표시한다")
    void ofUsesTotalPagesAndSafeAdminFallback() {
        AdminActivityLogListResDto row = new AdminActivityLogListResDto();
        row.setLogNo(21L);
        row.setAdminNo(null);
        row.setActionType("TASK_UPDATE");
        row.setTargetId(3L);
        row.setIpAddress("127.0.0.1");
        row.setActionDtm(LocalDateTime.of(2026, 6, 6, 9, 30));

        AdminLogListResponse response = AdminLogListResponse.of(
                new PageImpl<>(List.of(row), PageRequest.of(1, 10), 21),
                new AdminActivityLogListQuery(null, "운영", null, null, null, null),
                Map.of(),
                new AdminActivityLogSummaryDto(21, 3, 0, 5, 0, 2)
        );

        assertEquals("관리자", response.items().get(0).adminName());
        assertEquals("11-11 / 21건 · 3페이지", response.pageInfoLabel());
        assertEquals("11-11 / 21건 · 3페이지", response.resultMeta().pageInfoLabel());
        assertEquals(1, response.resultMeta().filterCount());
    }
}
