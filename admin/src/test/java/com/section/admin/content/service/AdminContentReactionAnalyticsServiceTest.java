package com.section.admin.content.service;

import com.section.admin.content.res.ContentReactionAnalyticsResponse;
import com.section.admin.content.res.ContentReactionDataQualityResponse;
import com.section.admin.content.res.ContentReactionDetailResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.content.dto.ContentReactionAnalyticsSummaryRow;
import com.section.common.content.dto.ContentReactionDataQualityRow;
import com.section.common.content.dto.ContentReactionSummaryRow;
import com.section.common.content.dto.ContentReactionTopRow;
import com.section.common.content.dto.ContentReactionTrendRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.FrontContentReactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminContentReactionAnalyticsServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-24T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private final FrontContentReactionRepository repository = mock(FrontContentReactionRepository.class);
    private final AdminContentReactionAnalyticsService service =
            new AdminContentReactionAnalyticsService(repository, FIXED_CLOCK);

    @Test
    @DisplayName("반응 분석은 기간 경계와 게시판을 적용하고 누락 날짜를 채운다")
    void returnsFilteredAnalyticsAndFillsDates() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 18, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 25, 0, 0);
        when(repository.getAnalyticsSummary(start, end, Document.BoardType.NOTICE))
                .thenReturn(new ContentReactionAnalyticsSummaryRow(10, 7, 3, 8, 2));
        when(repository.getDailyReactionTrend(start, end, Document.BoardType.NOTICE))
                .thenReturn(List.of(
                        new ContentReactionTrendRow(LocalDate.of(2026, 7, 18), 2, 1),
                        new ContentReactionTrendRow(LocalDate.of(2026, 7, 24), 5, 2)
                ));
        when(repository.getTopReactedContents(start, end, Document.BoardType.NOTICE, 50))
                .thenReturn(List.of(
                        new ContentReactionTopRow(1, Document.BoardType.NOTICE, "배송", 5, 1),
                        new ContentReactionTopRow(2, Document.BoardType.NOTICE, "검수", 2, 2)
                ));

        ContentReactionAnalyticsResponse response =
                service.getAnalytics(Document.BoardType.NOTICE, 7);

        assertThat(response.startDate()).isEqualTo("2026-07-18");
        assertThat(response.endDate()).isEqualTo("2026-07-24");
        assertThat(response.summary().helpfulRate()).isEqualTo(70);
        assertThat(response.trend()).hasSize(7);
        assertThat(response.trend().get(1).totalCount()).isZero();
        assertThat(response.trend().get(6).helpfulRate()).isEqualTo(71);
        assertThat(response.topContents()).extracting(ContentReactionAnalyticsResponse.Content::documentId)
                .containsExactly(1L, 2L);
        assertThat(response.improvementContents()).extracting(ContentReactionAnalyticsResponse.Content::documentId)
                .containsExactly(2L, 1L);
        verify(repository).getTopReactedContents(start, end, Document.BoardType.NOTICE, 50);
    }

    @Test
    @DisplayName("반응이 없으면 도움 비율은 0이고 개선 후보도 비어 있다")
    void returnsZeroRateForEmptyAnalytics() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 11, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 25, 0, 0);
        when(repository.getAnalyticsSummary(start, end, null))
                .thenReturn(new ContentReactionAnalyticsSummaryRow(0, 0, 0, 0, 0));
        when(repository.getDailyReactionTrend(start, end, null)).thenReturn(List.of());
        when(repository.getTopReactedContents(start, end, null, 50)).thenReturn(List.of());

        ContentReactionAnalyticsResponse response = service.getAnalytics(null, 14);

        assertThat(response.boardType()).isEqualTo("ALL");
        assertThat(response.summary().helpfulRate()).isZero();
        assertThat(response.trend()).hasSize(14);
        assertThat(response.improvementContents()).isEmpty();
    }

    @Test
    @DisplayName("반응 분석은 지원하지 않는 기간을 거부한다")
    void rejectsUnsupportedRange() {
        assertThatThrownBy(() -> service.getAnalytics(Document.BoardType.STYLE, 10))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("반응 데이터 품질은 고아 반응 유무와 수집 기간을 운영 상태로 변환한다")
    void returnsReactionDataQuality() {
        when(repository.getDataQuality()).thenReturn(new ContentReactionDataQualityRow(
                10,
                8,
                LocalDateTime.of(2026, 7, 1, 9, 0),
                LocalDateTime.of(2026, 7, 24, 11, 0)
        ));

        ContentReactionDataQualityResponse response = service.getDataQuality();

        assertThat(response.totalCount()).isEqualTo(10);
        assertThat(response.orphanCount()).isEqualTo(2);
        assertThat(response.status()).isEqualTo("CLEANUP_REQUIRED");
        assertThat(response.oldestReactedAt()).isEqualTo("2026-07-01 09:00:00");
    }

    @Test
    @DisplayName("문서 반응 인사이트는 전체 누계와 최근 활동을 분리하고 개선 상태를 계산한다")
    void returnsDocumentReactionInsight() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 25, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 25, 0, 0);
        when(repository.getSummary(31L)).thenReturn(new ContentReactionSummaryRow(2, 3));
        when(repository.getDailyReactionTrend(31L, start, end)).thenReturn(List.of(
                new ContentReactionTrendRow(LocalDate.of(2026, 7, 23), 1, 1),
                new ContentReactionTrendRow(LocalDate.of(2026, 7, 24), 0, 1)
        ));

        ContentReactionDetailResponse response = service.getDocumentInsight(31L, 30);

        assertThat(response.totalCount()).isEqualTo(5);
        assertThat(response.helpfulRate()).isEqualTo(40);
        assertThat(response.recentActivityCount()).isEqualTo(3);
        assertThat(response.status()).isEqualTo("IMPROVEMENT_REQUIRED");
        assertThat(response.trend()).hasSize(30);
        assertThat(response.trend().get(28).totalCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("문서 반응 인사이트는 7일, 30일, 90일 이외 기간을 거부한다")
    void rejectsUnsupportedDocumentRange() {
        assertThatThrownBy(() -> service.getDocumentInsight(1L, 14))
                .isInstanceOf(BusinessException.class);
    }
}
