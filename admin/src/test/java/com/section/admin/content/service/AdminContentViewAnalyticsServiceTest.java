package com.section.admin.content.service;

import com.section.admin.content.res.ContentViewAnalyticsResponse;
import com.section.admin.content.res.ContentViewDataQualityResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.content.dto.ContentViewDataQualityRow;
import com.section.common.content.dto.ContentViewSummaryRow;
import com.section.common.content.dto.ContentViewTopRow;
import com.section.common.content.dto.ContentViewTrendRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.FrontContentViewEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminContentViewAnalyticsServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-23T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private final FrontContentViewEventRepository repository = mock(FrontContentViewEventRepository.class);
    private final AdminContentViewAnalyticsService service =
            new AdminContentViewAnalyticsService(repository, FIXED_CLOCK);

    @Test
    @DisplayName("조회 분석은 현재 기간과 직전 동일 기간을 비교하고 누락 날짜를 0으로 채운다")
    void returnsComparisonAndFillsMissingTrendDates() {
        LocalDate startDate = LocalDate.of(2026, 7, 17);
        LocalDate endDate = LocalDate.of(2026, 7, 23);
        when(repository.getViewSummary(startDate, endDate, Document.BoardType.NOTICE))
                .thenReturn(new ContentViewSummaryRow(15, 9, 3));
        when(repository.getViewSummary(
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 16),
                Document.BoardType.NOTICE
        )).thenReturn(new ContentViewSummaryRow(10, 7, 2));
        when(repository.getDailyViewTrend(startDate, endDate, Document.BoardType.NOTICE))
                .thenReturn(List.of(
                        new ContentViewTrendRow(LocalDate.of(2026, 7, 17), 4, 3),
                        new ContentViewTrendRow(LocalDate.of(2026, 7, 23), 11, 6)
                ));
        when(repository.getTopViewedContents(startDate, endDate, Document.BoardType.NOTICE, 5))
                .thenReturn(List.of(new ContentViewTopRow(
                        11L, Document.BoardType.NOTICE, "배송 공지", 8, 5
                )));

        ContentViewAnalyticsResponse response = service.getAnalytics(Document.BoardType.NOTICE, 7);

        assertThat(response.boardType()).isEqualTo("NOTICE");
        assertThat(response.startDate()).isEqualTo("2026-07-17");
        assertThat(response.endDate()).isEqualTo("2026-07-23");
        assertThat(response.summary().averageViewsPerContent()).isEqualTo(5.0);
        assertThat(response.summary().viewChangeRate()).isEqualTo(50);
        assertThat(response.trend()).hasSize(7);
        assertThat(response.trend().get(1).viewCount()).isZero();
        assertThat(response.trend().get(6).viewCount()).isEqualTo(11);
        assertThat(response.topContents()).singleElement().satisfies(item -> {
            assertThat(item.documentId()).isEqualTo(11L);
            assertThat(item.uniqueVisitors()).isEqualTo(5);
        });
    }

    @Test
    @DisplayName("직전 기간 조회가 없고 현재 조회가 있으면 증감률을 100으로 표시한다")
    void returnsOneHundredPercentWhenPreviousViewsAreZero() {
        LocalDate startDate = LocalDate.of(2026, 7, 10);
        LocalDate endDate = LocalDate.of(2026, 7, 23);
        when(repository.getViewSummary(startDate, endDate, null))
                .thenReturn(new ContentViewSummaryRow(3, 2, 1));
        when(repository.getViewSummary(
                LocalDate.of(2026, 6, 26),
                LocalDate.of(2026, 7, 9),
                null
        )).thenReturn(new ContentViewSummaryRow(0, 0, 0));
        when(repository.getDailyViewTrend(startDate, endDate, null)).thenReturn(List.of());
        when(repository.getTopViewedContents(startDate, endDate, null, 5)).thenReturn(List.of());

        ContentViewAnalyticsResponse response = service.getAnalytics(null, 14);

        assertThat(response.boardType()).isEqualTo("ALL");
        assertThat(response.summary().viewChangeRate()).isEqualTo(100);
        assertThat(response.trend()).hasSize(14).allSatisfy(item ->
                assertThat(item.viewCount()).isZero()
        );
        verify(repository).getTopViewedContents(startDate, endDate, null, 5);
    }

    @Test
    @DisplayName("조회 분석은 7일, 14일, 30일 이외 기간을 거부한다")
    void rejectsUnsupportedRangeDays() {
        assertThatThrownBy(() -> service.getAnalytics(Document.BoardType.QNA, 10))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("조회 이벤트 품질은 고아 이벤트 유무에 따라 운영 상태를 구분한다")
    void returnsDataQualityStatus() {
        when(repository.getDataQuality()).thenReturn(new ContentViewDataQualityRow(
                100,
                97,
                3,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 7, 23)
        ));

        ContentViewDataQualityResponse response = service.getDataQuality();

        assertThat(response.totalEventCount()).isEqualTo(100);
        assertThat(response.validEventCount()).isEqualTo(97);
        assertThat(response.orphanEventCount()).isEqualTo(3);
        assertThat(response.status()).isEqualTo("CLEANUP_REQUIRED");
        assertThat(response.oldestViewedDate()).isEqualTo("2026-01-01");
        assertThat(response.latestViewedDate()).isEqualTo("2026-07-23");
    }

    @Test
    @DisplayName("조회 이벤트가 없으면 정상 상태와 빈 수집 기간을 반환한다")
    void returnsHealthyStatusWhenEventsAreEmpty() {
        when(repository.getDataQuality())
                .thenReturn(new ContentViewDataQualityRow(0, 0, 0, null, null));

        ContentViewDataQualityResponse response = service.getDataQuality();

        assertThat(response.status()).isEqualTo("HEALTHY");
        assertThat(response.oldestViewedDate()).isNull();
        assertThat(response.latestViewedDate()).isNull();
    }
}
