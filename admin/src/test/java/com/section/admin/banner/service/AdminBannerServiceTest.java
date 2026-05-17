package com.section.admin.banner.service;

import com.section.admin.banner.req.BannerListRequest;
import com.section.admin.banner.req.BannerSaveRequest;
import com.section.admin.banner.res.BannerListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.commerce.dto.BannerListResDto;
import com.section.common.commerce.entity.DisplayBanner;
import com.section.common.commerce.repository.BannerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBannerServiceTest {

    @Mock
    private BannerRepository bannerRepository;
    @Mock
    private AdminBannerMapper adminBannerMapper;

    @InjectMocks
    private AdminBannerService adminBannerService;

    @Test
    @DisplayName("배너 목록은 운영용 응답 DTO로 반환한다")
    void getBannerListReturnsDtoResponse() {
        BannerListRequest request = new BannerListRequest();
        BannerListResDto row = new BannerListResDto();
        row.setBannerNo(1L);
        row.setTitle("메인 배너");
        row.setIsActive("Y");
        row.setSortOrder(1);
        row.setStartDtm(LocalDateTime.now().minusDays(1));
        row.setEndDtm(LocalDateTime.now().plusDays(1));

        when(bannerRepository.getBannerList(any())).thenReturn(List.of(row));

        BannerListResponse response = adminBannerService.getBannerList(request);

        assertEquals(1, response.items().size());
        assertEquals("메인 배너", response.items().get(0).title());
        assertEquals("전체 1건", response.resultMeta().resultLabel());
        assertEquals("정렬 순서 기준", response.resultMeta().querySignature());
    }

    @Test
    @DisplayName("배너 저장은 잘못된 기간 요청을 거부한다")
    void saveBannerRejectsInvalidPeriod() {
        BannerSaveRequest request = new BannerSaveRequest(
                null,
                "테스트",
                "https://example.com/image.png",
                null,
                LocalDateTime.of(2026, 5, 11, 10, 0),
                LocalDateTime.of(2026, 5, 10, 10, 0),
                1,
                "Y"
        );

        assertThrows(BusinessException.class, () -> adminBannerService.saveBanner(request));
    }

    @Test
    @DisplayName("배너 상태 변경은 활성값만 바꾼다")
    void updateActiveChangesActiveFlag() {
        DisplayBanner banner = DisplayBanner.builder()
                .bannerNo(3L)
                .title("배너")
                .imageUrl("https://example.com/a.png")
                .targetUrl(null)
                .startDtm(LocalDateTime.of(2026, 5, 11, 10, 0))
                .endDtm(LocalDateTime.of(2026, 5, 20, 10, 0))
                .sortOrder(1)
                .isActive("Y")
                .crtAdminNo(1L)
                .build();

        when(bannerRepository.findById(3L)).thenReturn(Optional.of(banner));

        adminBannerService.updateActive(3L, "N");

        assertEquals("N", banner.getIsActive());
    }
}
