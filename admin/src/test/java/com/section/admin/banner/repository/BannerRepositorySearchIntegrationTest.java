package com.section.admin.banner.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.commerce.entity.DisplayBanner;
import com.section.common.commerce.repository.BannerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
        DisplayBanner liveBanner = bannerRepository.save(DisplayBanner.builder()
                .title("라이브 배너")
                .imageUrl("https://example.com/live.png")
                .startDtm(LocalDateTime.of(2026, 6, 1, 9, 0))
                .endDtm(LocalDateTime.of(2026, 6, 7, 23, 0))
                .sortOrder(1)
                .isActive("Y")
                .crtAdminNo(1L)
                .build());
        bannerRepository.save(DisplayBanner.builder()
                .title("보관 배너")
                .imageUrl("https://example.com/archive.png")
                .startDtm(LocalDateTime.of(2026, 6, 2, 9, 0))
                .endDtm(LocalDateTime.of(2026, 6, 3, 23, 0))
                .sortOrder(1)
                .isActive("N")
                .crtAdminNo(1L)
                .build());

        boolean conflicted = bannerRepository.existsActiveBannerScheduleConflict(
                null,
                1,
                LocalDateTime.of(2026, 6, 3, 0, 0),
                LocalDateTime.of(2026, 6, 5, 0, 0)
        );
        boolean selfExcluded = bannerRepository.existsActiveBannerScheduleConflict(
                liveBanner.getBannerNo(),
                1,
                LocalDateTime.of(2026, 6, 3, 0, 0),
                LocalDateTime.of(2026, 6, 5, 0, 0)
        );
        boolean differentSortOrder = bannerRepository.existsActiveBannerScheduleConflict(
                null,
                2,
                LocalDateTime.of(2026, 6, 3, 0, 0),
                LocalDateTime.of(2026, 6, 5, 0, 0)
        );

        assertTrue(conflicted);
        assertFalse(selfExcluded);
        assertFalse(differentSortOrder);
    }
}
