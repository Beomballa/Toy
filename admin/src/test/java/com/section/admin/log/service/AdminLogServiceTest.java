package com.section.admin.log.service;

import com.section.admin.log.req.AdminLogListRequest;
import com.section.admin.log.res.AdminLogDetailResponse;
import com.section.admin.log.res.AdminLogListResponse;
import com.section.common.system.dto.AdminActivityLogListResDto;
import com.section.common.system.dto.AdminActivityLogSummaryDto;
import com.section.common.system.entity.AdminActivityLog;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminActivityLogRepository;
import com.section.common.system.repository.AdminUserRepository;
import com.section.common.system.support.AdminRequestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLogServiceTest {

    @Mock
    private AdminActivityLogRepository adminActivityLogRepository;
    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AdminLogService adminLogService;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        AdminRequestContext.clear();
    }

    @Test
    @DisplayName("활동 로그 목록은 작업자명을 포함한 페이지 응답을 반환한다")
    void getLogListReturnsPagedResponse() {
        AdminActivityLogListResDto row = new AdminActivityLogListResDto();
        row.setLogNo(1L);
        row.setAdminNo(2L);
        row.setActionType("PRODUCT_UPDATE");
        row.setTargetId(4L);
        row.setIpAddress("127.0.0.1");
        row.setActionDtm(LocalDateTime.of(2026, 5, 11, 12, 0));

        when(adminActivityLogRepository.getLogList(any(), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));
        when(adminActivityLogRepository.getLogSummary(any()))
                .thenReturn(new AdminActivityLogSummaryDto(1, 1, 0, 0, 1, 1));
        when(adminUserRepository.findAllById(any()))
                .thenReturn(List.of(AdminUser.builder().adminNo(2L).name("운영자").loginId("ops").password("pw").build()));

        AdminLogListResponse response = adminLogService.getLogList(new AdminLogListRequest(), PageRequest.of(0, 20));

        assertEquals(1, response.items().size());
        assertEquals("운영자", response.items().get(0).adminName());
        assertEquals("상품 #4", response.items().get(0).targetLabel());
        assertEquals("/admin/products/history?productNo=4", response.items().get(0).targetPath());
        assertEquals(0, response.currentPage());
        assertEquals(20, response.pageSize());
        assertEquals("1-1 / 1건 · 1페이지", response.pageInfoLabel());
        assertEquals("검색 결과 1건", response.resultMeta().resultLabel());
        assertEquals(1, response.summary().totalCount());
        assertEquals(1, response.summary().commerceCount());
    }

    @Test
    @DisplayName("활동 로그 상세는 작업자명을 포함해 반환한다")
    void getLogDetailReturnsActorName() {
        AdminActivityLog log = AdminActivityLog.builder()
                .adminNo(3L)
                .actionType("BANNER_DELETE")
                .targetId(7L)
                .ipAddress("127.0.0.1")
                .build();
        try {
            java.lang.reflect.Field logNoField = AdminActivityLog.class.getDeclaredField("logNo");
            logNoField.setAccessible(true);
            logNoField.set(log, 9L);
            java.lang.reflect.Field actionDtmField = AdminActivityLog.class.getDeclaredField("actionDtm");
            actionDtmField.setAccessible(true);
            actionDtmField.set(log, LocalDateTime.of(2026, 5, 11, 13, 0));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        when(adminActivityLogRepository.findById(9L)).thenReturn(Optional.of(log));
        when(adminUserRepository.findById(3L))
                .thenReturn(Optional.of(AdminUser.builder().adminNo(3L).name("배너담당").loginId("banner").password("pw").build()));

        AdminLogDetailResponse response = adminLogService.getLogDetail(9L);

        assertEquals("배너담당", response.adminName());
        assertEquals("BANNER_DELETE", response.actionType());
        assertEquals("배너 #7", response.targetLabel());
        assertEquals("/admin/banner/list", response.targetPath());
    }

    @Test
    @DisplayName("현재 요청 컨텍스트 기준 활동 로그를 저장한다")
    void recordCurrentAdminLogUsesRequestContext() {
        AdminRequestContext.setCurrentAdminNo(11L);
        AdminRequestContext.setCurrentIpAddress("10.0.0.5");

        adminLogService.recordCurrentAdminLog("NOTICE_CREATE", 3L);

        verify(adminActivityLogRepository).save(any(AdminActivityLog.class));
    }
}
