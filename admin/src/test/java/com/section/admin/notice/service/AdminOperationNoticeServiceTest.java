package com.section.admin.notice.service;

import com.section.admin.log.service.AdminLogService;
import com.section.admin.notice.req.AdminOperationNoticeListRequest;
import com.section.admin.notice.req.AdminOperationNoticeSaveRequest;
import com.section.admin.notice.res.AdminOperationNoticeListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.system.dto.AdminOperationNoticeListQuery;
import com.section.common.system.dto.AdminOperationNoticeListResDto;
import com.section.common.system.entity.AdminOperationNotice;
import com.section.common.system.repository.AdminOperationNoticeRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOperationNoticeServiceTest {

    @Mock
    private AdminOperationNoticeRepository adminOperationNoticeRepository;
    @Mock
    private AdminLogService adminLogService;

    @InjectMocks
    private AdminOperationNoticeService adminOperationNoticeService;

    @Test
    @DisplayName("운영 공지 목록은 페이지 응답과 메타를 반환한다")
    void getNoticeListReturnsPagedResponse() {
        AdminOperationNoticeListRequest request = new AdminOperationNoticeListRequest();
        request.setKeyword("점검");
        request.setIsActive("Y");

        AdminOperationNoticeListResDto row = new AdminOperationNoticeListResDto();
        row.setNoticeNo(1L);
        row.setTitle("점검 공지");
        row.setContent("점검 내용");
        row.setIsActive("Y");
        row.setIsPinned("Y");
        row.setCrtDtm(LocalDateTime.now());

        when(adminOperationNoticeRepository.getNoticeList(any(AdminOperationNoticeListQuery.class), any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1));

        AdminOperationNoticeListResponse response = adminOperationNoticeService.getNoticeList(request);

        assertEquals(1, response.items().size());
        assertEquals(0, response.currentPage());
        assertEquals(10, response.pageSize());
        assertEquals(1L, response.totalElements());
        assertEquals("검색 결과 1건", response.resultMeta().resultLabel());
    }

    @Test
    @DisplayName("운영 공지 저장은 종료일이 시작일보다 빠르면 거부한다")
    void saveNoticeRejectsInvalidPeriod() {
        AdminOperationNoticeSaveRequest request = new AdminOperationNoticeSaveRequest(
                null,
                "점검 공지",
                "점검 내용",
                "Y",
                "N",
                LocalDateTime.of(2026, 5, 20, 9, 0),
                LocalDateTime.of(2026, 5, 19, 9, 0)
        );

        assertThrows(BusinessException.class, () -> adminOperationNoticeService.saveNotice(request));
    }

    @Test
    @DisplayName("운영 공지 상태 변경은 활성값만 갱신한다")
    void updateActiveChangesStatus() {
        AdminOperationNotice notice = AdminOperationNotice.builder()
                .noticeNo(3L)
                .title("점검")
                .content("점검 내용")
                .isActive("Y")
                .isPinned("N")
                .build();
        when(adminOperationNoticeRepository.findById(3L)).thenReturn(Optional.of(notice));

        adminOperationNoticeService.updateActive(3L, "N");

        assertEquals("N", notice.getIsActive());
        verify(adminLogService).recordCurrentAdminLog("NOTICE_ACTIVE_UPDATE", 3L);
    }

    @Test
    @DisplayName("신규 운영 공지 저장은 저장소에 새 엔티티를 위임한다")
    void saveNoticeCreatesEntity() {
        AdminOperationNoticeSaveRequest request = new AdminOperationNoticeSaveRequest(
                null,
                "  긴급 공지 ",
                "  내용  정리 ",
                "Y",
                "Y",
                null,
                null
        );
        when(adminOperationNoticeRepository.save(any(AdminOperationNotice.class)))
                .thenAnswer(invocation -> {
                    AdminOperationNotice entity = invocation.getArgument(0);
                    try {
                        java.lang.reflect.Field noticeNoField = AdminOperationNotice.class.getDeclaredField("noticeNo");
                        noticeNoField.setAccessible(true);
                        noticeNoField.set(entity, 15L);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                    return entity;
                });

        adminOperationNoticeService.saveNotice(request);

        verify(adminOperationNoticeRepository).save(any(AdminOperationNotice.class));
        verify(adminLogService).recordCurrentAdminLog("NOTICE_CREATE", 15L);
    }

    @Test
    @DisplayName("기존 운영 공지 수정은 활동 로그를 남긴다")
    void saveNoticeUpdateRecordsLog() {
        AdminOperationNotice notice = AdminOperationNotice.builder()
                .noticeNo(7L)
                .title("기존")
                .content("내용")
                .isActive("Y")
                .isPinned("N")
                .build();
        when(adminOperationNoticeRepository.findById(7L)).thenReturn(Optional.of(notice));

        adminOperationNoticeService.saveNotice(new AdminOperationNoticeSaveRequest(
                7L,
                "수정 공지",
                "수정 내용",
                "Y",
                "Y",
                null,
                null
        ));

        verify(adminLogService).recordCurrentAdminLog("NOTICE_UPDATE", 7L);
    }
}
