package com.section.admin.banner.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.commerce.dto.BannerListQuery;
import com.section.common.commerce.dto.BannerSummaryDto;
import com.section.common.commerce.entity.DisplayBanner;
import com.section.common.commerce.repository.BannerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class BannerRepositorySearchIntegrationTest {

    @Autowired
    private BannerRepository bannerRepository;

    @Test
    @DisplayName("활성 배너 노출 충돌 검사는 같은 정렬 순서의 기간 겹침만 감지한다")
    void existsActiveBannerScheduleConflictDetectsOverlappingActiveWindow() {
        int uniqueSortOrder = 991;
        DisplayBanner liveBanner = bannerRepository.save(DisplayBanner.builder()
                .title("라이브 배너")
                .imageUrl("https://example.com/live.png")
                .startDtm(LocalDateTime.of(2026, 6, 1, 9, 0))
                .endDtm(LocalDateTime.of(2026, 6, 7, 23, 0))
                .sortOrder(uniqueSortOrder)
                .isActive("Y")
                .crtAdminNo(1L)
                .build());
        bannerRepository.save(DisplayBanner.builder()
                .title("보관 배너")
                .imageUrl("https://example.com/archive.png")
                .startDtm(LocalDateTime.of(2026, 6, 2, 9, 0))
                .endDtm(LocalDateTime.of(2026, 6, 3, 23, 0))
                .sortOrder(uniqueSortOrder)
                .isActive("N")
                .crtAdminNo(1L)
                .build());

        boolean conflicted = bannerRepository.existsActiveBannerScheduleConflict(
                null,
                uniqueSortOrder,
                LocalDateTime.of(2026, 6, 3, 0, 0),
                LocalDateTime.of(2026, 6, 5, 0, 0)
        );
        boolean selfExcluded = bannerRepository.existsActiveBannerScheduleConflict(
                liveBanner.getBannerNo(),
                uniqueSortOrder,
                LocalDateTime.of(2026, 6, 3, 0, 0),
                LocalDateTime.of(2026, 6, 5, 0, 0)
        );
        boolean differentSortOrder = bannerRepository.existsActiveBannerScheduleConflict(
                null,
                uniqueSortOrder + 1,
                LocalDateTime.of(2026, 6, 3, 0, 0),
                LocalDateTime.of(2026, 6, 5, 0, 0)
        );

        assertTrue(conflicted);
        assertFalse(selfExcluded);
        assertFalse(differentSortOrder);
    }

    @Test
    @DisplayName("배너 요약 집계는 대기, 노출중, 종료, 중지 축을 분리한다")
    void getBannerSummarySeparatesOperationalBuckets() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 13, 12, 0);
        String keyword = "요약검증";

        bannerRepository.save(DisplayBanner.builder()
                .title(keyword + " 라이브 배너")
                .imageUrl("https://example.com/live.png")
                .startDtm(now.minusDays(1))
                .endDtm(now.plusDays(1))
                .sortOrder(992)
                .isActive("Y")
                .crtAdminNo(1L)
                .build());
        bannerRepository.save(DisplayBanner.builder()
                .title(keyword + " 대기 배너")
                .imageUrl("https://example.com/scheduled.png")
                .startDtm(now.plusDays(1))
                .endDtm(now.plusDays(2))
                .sortOrder(993)
                .isActive("Y")
                .crtAdminNo(1L)
                .build());
        bannerRepository.save(DisplayBanner.builder()
                .title(keyword + " 종료 배너")
                .imageUrl("https://example.com/ended.png")
                .startDtm(now.minusDays(4))
                .endDtm(now.minusDays(1))
                .sortOrder(994)
                .isActive("Y")
                .crtAdminNo(1L)
                .build());
        bannerRepository.save(DisplayBanner.builder()
                .title(keyword + " 중지 배너")
                .imageUrl("https://example.com/inactive.png")
                .startDtm(now.minusDays(2))
                .endDtm(now.plusDays(2))
                .sortOrder(995)
                .isActive("N")
                .crtAdminNo(1L)
                .build());

        BannerSummaryDto summary = bannerRepository.getBannerSummary(new BannerListQuery(keyword, null, null), now);

        assertEquals(4, summary.totalCount());
        assertEquals(1, summary.liveCount());
        assertEquals(1, summary.scheduledCount());
        assertEquals(1, summary.endedCount());
        assertEquals(1, summary.inactiveCount());
    }
}
