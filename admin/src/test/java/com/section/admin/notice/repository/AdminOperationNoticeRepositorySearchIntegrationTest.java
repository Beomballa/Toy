package com.section.admin.notice.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.base.entity.type.AdminNoticeVisibilityStatus;
import com.section.common.system.dto.AdminOperationNoticeListQuery;
import com.section.common.system.dto.AdminOperationNoticeSummaryDto;
import com.section.common.system.entity.AdminOperationNotice;
import com.section.common.system.repository.AdminOperationNoticeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class AdminOperationNoticeRepositorySearchIntegrationTest {

    @Autowired
    private AdminOperationNoticeRepository adminOperationNoticeRepository;

    @Test
    @DisplayName("운영 공지 요약은 종료와 비활성 공지 건수를 분리해 집계한다")
    void getNoticeSummaryCountsEndedAndInactiveSeparately() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 13, 12, 0);
        adminOperationNoticeRepository.save(AdminOperationNotice.builder()
                .title("[repo-notice] live")
                .content("repo notice live")
                .isActive("Y")
                .isPinned("N")
                .startDtm(now.minusDays(1))
                .endDtm(now.plusDays(1))
                .build());
        adminOperationNoticeRepository.save(AdminOperationNotice.builder()
                .title("[repo-notice] scheduled")
                .content("repo notice scheduled")
                .isActive("Y")
                .isPinned("N")
                .startDtm(now.plusDays(1))
                .endDtm(now.plusDays(2))
                .build());
        adminOperationNoticeRepository.save(AdminOperationNotice.builder()
                .title("[repo-notice] ended")
                .content("repo notice ended")
                .isActive("Y")
                .isPinned("N")
                .startDtm(now.minusDays(3))
                .endDtm(now.minusDays(1))
                .build());
        adminOperationNoticeRepository.save(AdminOperationNotice.builder()
                .title("[repo-notice] inactive")
                .content("repo notice inactive")
                .isActive("N")
                .isPinned("N")
                .build());

        AdminOperationNoticeListQuery query = new AdminOperationNoticeListQuery("[repo-notice]", null, null, null);
        AdminOperationNoticeSummaryDto summary = adminOperationNoticeRepository.getNoticeSummary(query, now);

        assertEquals(4L, summary.totalCount());
        assertEquals(1L, summary.liveCount());
        assertEquals(1L, summary.scheduledCount());
        assertEquals(1L, summary.endedCount());
        assertEquals(1L, summary.inactiveCount());
    }

    @Test
    @DisplayName("운영 공지 목록은 종료 상태 필터로 종료 공지만 조회한다")
    void getNoticeListFiltersEndedStatus() {
        LocalDateTime now = LocalDateTime.now();
        adminOperationNoticeRepository.save(AdminOperationNotice.builder()
                .title("[repo-ended] live")
                .content("repo ended live")
                .isActive("Y")
                .isPinned("N")
                .startDtm(now.minusDays(1))
                .endDtm(now.plusDays(1))
                .build());
        AdminOperationNotice endedNotice = adminOperationNoticeRepository.save(AdminOperationNotice.builder()
                .title("[repo-ended] ended")
                .content("repo ended match")
                .isActive("Y")
                .isPinned("N")
                .startDtm(now.minusDays(5))
                .endDtm(now.minusDays(2))
                .build());

        var page = adminOperationNoticeRepository.getNoticeList(
                new AdminOperationNoticeListQuery("[repo-ended]", null, null, AdminNoticeVisibilityStatus.ENDED),
                PageRequest.of(0, 10)
        );

        assertEquals(1L, page.getTotalElements());
        assertEquals(endedNotice.getNoticeNo(), page.getContent().getFirst().getNoticeNo());
    }
}
