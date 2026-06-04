package com.section.admin.banner.service;

import com.section.admin.banner.req.BannerListRequest;
import com.section.admin.banner.req.BannerBulkDeleteRequest;
import com.section.admin.banner.req.BannerBulkOperateRequest;
import com.section.admin.banner.req.BannerSaveRequest;
import com.section.admin.banner.res.BannerDetailResponse;
import com.section.admin.banner.res.BannerListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.commerce.dto.BannerListQuery;
import com.section.common.commerce.dto.BannerListResDto;
import com.section.common.commerce.entity.DisplayBanner;
import com.section.common.commerce.repository.BannerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

        when(bannerRepository.getBannerList(any(), any())).thenReturn(new PageImpl<>(
                List.of(row),
                PageRequest.of(0, 10),
                1
        ));

        BannerListResponse response = adminBannerService.getBannerList(request);

        assertEquals(1, response.items().size());
        assertEquals(0, response.currentPage());
        assertEquals(10, response.pageSize());
        assertEquals(1, response.totalElements());
        assertEquals("메인 배너", response.items().get(0).title());
        assertEquals("전체 1건", response.resultMeta().resultLabel());
        assertEquals("정렬 순서 기준", response.resultMeta().querySignature());
    }

    @Test
    @DisplayName("배너 목록 요청은 노출 기간 필터를 Querydsl 조회 경계까지 전달한다")
    void getBannerListPassesExposureStatusFilter() {
        BannerListRequest request = new BannerListRequest();
        request.setExposureStatus("live");

        when(bannerRepository.getBannerList(any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        adminBannerService.getBannerList(request);

        ArgumentCaptor<BannerListQuery> queryCaptor =
                ArgumentCaptor.forClass(BannerListQuery.class);
        verify(bannerRepository).getBannerList(queryCaptor.capture(), any());
        assertEquals("LIVE", queryCaptor.getValue().exposureStatus());
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
    @DisplayName("활성 배너 저장은 같은 정렬 순서의 노출 기간 충돌을 거부한다")
    void saveBannerRejectsScheduleConflict() {
        BannerSaveRequest request = new BannerSaveRequest(
                null,
                "메인 배너",
                "https://example.com/image.png",
                null,
                LocalDateTime.of(2026, 6, 1, 9, 0),
                LocalDateTime.of(2026, 6, 7, 23, 0),
                1,
                "Y"
        );
        when(bannerRepository.existsActiveBannerScheduleConflict(null, 1, request.startDtm(), request.endDtm()))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> adminBannerService.saveBanner(request));
        verify(adminBannerMapper, never()).toEntity(any());
    }

    @Test
    @DisplayName("비활성 배너 저장은 노출 충돌 검사를 생략한다")
    void saveBannerSkipsConflictCheckForInactiveBanner() {
        BannerSaveRequest request = new BannerSaveRequest(
                null,
                "보관 배너",
                "https://example.com/image.png",
                null,
                LocalDateTime.of(2026, 6, 1, 9, 0),
                LocalDateTime.of(2026, 6, 7, 23, 0),
                9,
                "N"
        );
        DisplayBanner mappedBanner = DisplayBanner.builder()
                .title("보관 배너")
                .imageUrl("https://example.com/image.png")
                .startDtm(request.startDtm())
                .endDtm(request.endDtm())
                .sortOrder(9)
                .isActive("N")
                .crtAdminNo(1L)
                .build();
        when(adminBannerMapper.toEntity(any())).thenReturn(mappedBanner);

        adminBannerService.saveBanner(request);

        verify(bannerRepository, never()).existsActiveBannerScheduleConflict(any(), eq(9), any(), any());
        verify(bannerRepository).save(mappedBanner);
    }

    @Test
    @DisplayName("배너 상세는 운영 편집에 필요한 필드를 그대로 반환한다")
    void getBannerDetailReturnsEditableFields() {
        DisplayBanner banner = DisplayBanner.builder()
                .bannerNo(14L)
                .title("메인 히어로")
                .imageUrl("https://example.com/hero.png")
                .targetUrl("/products/hero")
                .startDtm(LocalDateTime.of(2026, 6, 1, 10, 0))
                .endDtm(LocalDateTime.of(2026, 6, 10, 10, 0))
                .sortOrder(1)
                .isActive("Y")
                .crtAdminNo(1L)
                .build();
        when(bannerRepository.findById(14L)).thenReturn(Optional.of(banner));

        BannerDetailResponse response = adminBannerService.getBannerDetail(14L);

        assertEquals(14L, response.bannerNo());
        assertEquals("메인 히어로", response.title());
        assertEquals("/products/hero", response.targetUrl());
        assertEquals("Y", response.isActive());
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

    @Test
    @DisplayName("비활성 배너 활성화는 노출 기간 충돌을 검증한다")
    void updateActiveRejectsScheduleConflict() {
        DisplayBanner banner = DisplayBanner.builder()
                .bannerNo(3L)
                .title("배너")
                .imageUrl("https://example.com/a.png")
                .targetUrl(null)
                .startDtm(LocalDateTime.of(2026, 5, 11, 10, 0))
                .endDtm(LocalDateTime.of(2026, 5, 20, 10, 0))
                .sortOrder(1)
                .isActive("N")
                .crtAdminNo(1L)
                .build();

        when(bannerRepository.findById(3L)).thenReturn(Optional.of(banner));
        when(bannerRepository.existsActiveBannerScheduleConflict(3L, 1, banner.getStartDtm(), banner.getEndDtm()))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> adminBannerService.updateActive(3L, "Y"));
    }

    @Test
    @DisplayName("배너 CSV 내보내기는 필터 결과를 파일 바이트로 변환한다")
    void exportBannerListCsvReturnsCsvBytes() {
        BannerListRequest request = new BannerListRequest();
        request.setIsActive("Y");
        request.setExposureStatus("LIVE");

        BannerListResDto row = new BannerListResDto();
        row.setBannerNo(8L);
        row.setSortOrder(2);
        row.setTitle("메인 배너");
        row.setIsActive("Y");
        row.setImageUrl("https://example.com/banner.png");
        row.setTargetUrl("/products/8");
        row.setStartDtm(LocalDateTime.now().minusDays(1));
        row.setEndDtm(LocalDateTime.now().plusDays(1));

        when(bannerRepository.getBannerList(any(), eq(PageRequest.of(0, 1000))))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 1000), 1));

        String csv = new String(adminBannerService.exportBannerListCsv(request), java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"조회조건\",\"상태: 사용 | 노출상태: 진행중\""));
        assertTrue(csv.contains("\"8\",\"2\",\"메인 배너\""));
    }

    @Test
    @DisplayName("배너 일괄 상태 변경은 변경 건수와 유지 건수를 구분한다")
    void bulkOperateUpdatesBanners() {
        DisplayBanner liveBanner = DisplayBanner.builder()
                .bannerNo(4L)
                .title("라이브 배너")
                .imageUrl("https://example.com/live.png")
                .startDtm(LocalDateTime.of(2026, 5, 11, 10, 0))
                .endDtm(LocalDateTime.of(2026, 5, 20, 10, 0))
                .sortOrder(1)
                .isActive("N")
                .crtAdminNo(1L)
                .build();
        DisplayBanner stoppedBanner = DisplayBanner.builder()
                .bannerNo(5L)
                .title("중지 배너")
                .imageUrl("https://example.com/stop.png")
                .startDtm(LocalDateTime.of(2026, 5, 11, 10, 0))
                .endDtm(LocalDateTime.of(2026, 5, 20, 10, 0))
                .sortOrder(4)
                .isActive("Y")
                .crtAdminNo(1L)
                .build();

        when(bannerRepository.findAllById(List.of(4L, 5L))).thenReturn(List.of(liveBanner, stoppedBanner));
        when(bannerRepository.existsActiveBannerScheduleConflict(4L, 1, liveBanner.getStartDtm(), liveBanner.getEndDtm()))
                .thenReturn(false);

        AdminBannerService.BulkOperateResult result = adminBannerService.bulkOperate(
                new BannerBulkOperateRequest(List.of(4L, 5L), "Y")
        );

        assertEquals(2, result.requestedCount());
        assertEquals(1, result.updatedCount());
        assertEquals(1, result.unchangedCount());
        assertEquals("Y", liveBanner.getIsActive());
    }

    @Test
    @DisplayName("배너 일괄 삭제는 누락 건수를 함께 반환한다")
    void bulkDeleteReturnsDeletedAndMissingCounts() {
        DisplayBanner banner = DisplayBanner.builder()
                .bannerNo(9L)
                .title("삭제 배너")
                .imageUrl("https://example.com/delete.png")
                .startDtm(LocalDateTime.of(2026, 5, 11, 10, 0))
                .endDtm(LocalDateTime.of(2026, 5, 20, 10, 0))
                .sortOrder(2)
                .isActive("Y")
                .crtAdminNo(1L)
                .build();
        when(bannerRepository.findAllById(List.of(9L, 10L))).thenReturn(List.of(banner));

        AdminBannerService.BulkDeleteResult result = adminBannerService.bulkDelete(
                new BannerBulkDeleteRequest(List.of(9L, 10L))
        );

        assertEquals(2, result.requestedCount());
        assertEquals(1, result.deletedCount());
        assertEquals(1, result.missingCount());
        verify(bannerRepository).deleteAll(List.of(banner));
    }

    @Test
    @DisplayName("배너 삭제는 존재하는 배너만 삭제한다")
    void deleteBannerDeletesExistingEntity() {
        DisplayBanner banner = DisplayBanner.builder()
                .bannerNo(9L)
                .title("삭제 배너")
                .imageUrl("https://example.com/delete.png")
                .targetUrl(null)
                .startDtm(LocalDateTime.of(2026, 5, 11, 10, 0))
                .endDtm(LocalDateTime.of(2026, 5, 20, 10, 0))
                .sortOrder(2)
                .isActive("Y")
                .crtAdminNo(1L)
                .build();
        when(bannerRepository.findById(9L)).thenReturn(Optional.of(banner));

        adminBannerService.deleteBanner(9L);

        verify(bannerRepository).delete(argThat(item -> item.getBannerNo().equals(9L)));
    }
}
