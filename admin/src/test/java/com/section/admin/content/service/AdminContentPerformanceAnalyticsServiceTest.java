package com.section.admin.content.service;

import com.section.admin.content.res.ContentPerformanceAnalyticsResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.content.dto.ContentReactionAnalyticsSummaryRow;
import com.section.common.content.dto.ContentReactionTopRow;
import com.section.common.content.dto.ContentViewSummaryRow;
import com.section.common.content.dto.ContentViewTopRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.FrontContentReactionRepository;
import com.section.common.content.repository.FrontContentViewEventRepository;
import com.section.common.system.entity.AdminOperationTask;
import com.section.common.system.repository.AdminOperationTaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminContentPerformanceAnalyticsServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-24T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private final FrontContentViewEventRepository viewRepository = mock(FrontContentViewEventRepository.class);
    private final FrontContentReactionRepository reactionRepository = mock(FrontContentReactionRepository.class);
    private final AdminOperationTaskRepository taskRepository = mock(AdminOperationTaskRepository.class);
    private final AdminContentPerformanceAnalyticsService service =
            new AdminContentPerformanceAnalyticsService(
                    viewRepository,
                    reactionRepository,
                    taskRepository,
                    FIXED_CLOCK
            );

    @Test
    @DisplayName("효과 분석은 전체 요약과 조회·반응 후보를 병합해 조치 우선순위를 계산한다")
    void mergesSignalsAndCalculatesPriority() {
        LocalDate startDate = LocalDate.of(2026, 7, 18);
        LocalDate endDate = LocalDate.of(2026, 7, 24);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        when(viewRepository.getViewSummary(startDate, endDate, Document.BoardType.NOTICE))
                .thenReturn(new ContentViewSummaryRow(100, 60, 3));
        when(reactionRepository.getAnalyticsSummary(start, end, Document.BoardType.NOTICE))
                .thenReturn(new ContentReactionAnalyticsSummaryRow(5, 2, 3, 5, 2));
        when(viewRepository.getTopViewedContents(startDate, endDate, Document.BoardType.NOTICE, 50))
                .thenReturn(List.of(
                        new ContentViewTopRow(1, Document.BoardType.NOTICE, "개선 공지", 50, 20),
                        new ContentViewTopRow(2, Document.BoardType.NOTICE, "무반응 공지", 20, 10),
                        new ContentViewTopRow(3, Document.BoardType.NOTICE, "관찰 공지", 10, 5)
                ));
        when(reactionRepository.getTopReactedContents(start, end, Document.BoardType.NOTICE, 50))
                .thenReturn(List.of(
                        new ContentReactionTopRow(1, Document.BoardType.NOTICE, "개선 공지", 1, 3),
                        new ContentReactionTopRow(4, Document.BoardType.NOTICE, "반응 전용", 1, 0)
                ));
        when(taskRepository.findAllBySourceTypeAndSourceIdIn(
                "CONTENT_PERFORMANCE",
                Set.of(1L, 2L, 3L, 4L)
        )).thenReturn(List.of(AdminOperationTask.builder()
                .taskNo(91L)
                .sourceType("CONTENT_PERFORMANCE")
                .sourceId(1L)
                .build()));

        ContentPerformanceAnalyticsResponse response =
                service.getAnalytics(Document.BoardType.NOTICE, 7);

        assertThat(response.summary().totalViews()).isEqualTo(100);
        assertThat(response.summary().totalReactions()).isEqualTo(5);
        assertThat(response.summary().helpfulRate()).isEqualTo(40);
        assertThat(response.summary().reactionCoverageRate()).isEqualTo(5);
        assertThat(response.summary().analyzedContentCount()).isEqualTo(4);
        assertThat(response.summary().actionRequiredCount()).isEqualTo(3);
        assertThat(response.summary().linkedActionCount()).isEqualTo(1);
        assertThat(response.summary().unlinkedActionCount()).isEqualTo(2);
        assertThat(response.priorityContents()).extracting(ContentPerformanceAnalyticsResponse.Content::documentId)
                .containsExactly(1L, 2L, 3L, 4L);
        assertThat(response.priorityContents().get(0).status()).isEqualTo("IMPROVEMENT_REQUIRED");
        assertThat(response.priorityContents().get(0).operationTaskNo()).isEqualTo(91L);
        assertThat(response.priorityContents().get(0).operationTaskPath()).contains("taskNo=91");
        assertThat(response.priorityContents().get(1).status()).isEqualTo("FEEDBACK_NEEDED");
        assertThat(response.priorityContents().get(3).status()).isEqualTo("LOW_SIGNAL");
    }

    @Test
    @DisplayName("효과 분석은 신호가 없을 때 0 요약과 빈 우선순위를 반환한다")
    void returnsEmptyAnalytics() {
        LocalDate startDate = LocalDate.of(2026, 7, 11);
        LocalDate endDate = LocalDate.of(2026, 7, 24);
        when(viewRepository.getViewSummary(startDate, endDate, null))
                .thenReturn(new ContentViewSummaryRow(0, 0, 0));
        when(reactionRepository.getAnalyticsSummary(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                null
        )).thenReturn(new ContentReactionAnalyticsSummaryRow(0, 0, 0, 0, 0));
        when(viewRepository.getTopViewedContents(startDate, endDate, null, 50)).thenReturn(List.of());
        when(reactionRepository.getTopReactedContents(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                null,
                50
        )).thenReturn(List.of());

        ContentPerformanceAnalyticsResponse response = service.getAnalytics(null, 14);

        assertThat(response.boardType()).isEqualTo("ALL");
        assertThat(response.summary().reactionCoverageRate()).isZero();
        assertThat(response.priorityContents()).isEmpty();
    }

    @Test
    @DisplayName("효과 분석은 7일, 14일, 30일 이외 기간을 거부한다")
    void rejectsUnsupportedRange() {
        assertThatThrownBy(() -> service.getAnalytics(Document.BoardType.STYLE, 90))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("조치·연결 지표는 실제 표시 및 일괄 처리 대상인 상위 10개 기준으로 계산한다")
    void calculatesActionCountsFromDisplayedPriorities() {
        LocalDate startDate = LocalDate.of(2026, 7, 18);
        LocalDate endDate = LocalDate.of(2026, 7, 24);
        when(viewRepository.getViewSummary(startDate, endDate, Document.BoardType.NOTICE))
                .thenReturn(new ContentViewSummaryRow(110, 80, 11));
        when(reactionRepository.getAnalyticsSummary(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                Document.BoardType.NOTICE
        )).thenReturn(new ContentReactionAnalyticsSummaryRow(0, 0, 0, 0, 0));
        List<ContentViewTopRow> rows = LongStream.rangeClosed(1, 11)
                .mapToObj(id -> new ContentViewTopRow(
                        id,
                        Document.BoardType.NOTICE,
                        "무반응 공지 " + id,
                        20 - id,
                        10
                ))
                .toList();
        when(viewRepository.getTopViewedContents(startDate, endDate, Document.BoardType.NOTICE, 50))
                .thenReturn(rows);
        when(reactionRepository.getTopReactedContents(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                Document.BoardType.NOTICE,
                50
        )).thenReturn(List.of());
        when(taskRepository.findAllBySourceTypeAndSourceIdIn(
                "CONTENT_PERFORMANCE",
                rows.stream().map(ContentViewTopRow::documentId).collect(java.util.stream.Collectors.toSet())
        )).thenReturn(List.of());

        ContentPerformanceAnalyticsResponse response =
                service.getAnalytics(Document.BoardType.NOTICE, 7);

        assertThat(response.summary().analyzedContentCount()).isEqualTo(11);
        assertThat(response.summary().actionRequiredCount()).isEqualTo(10);
        assertThat(response.summary().unlinkedActionCount()).isEqualTo(10);
        assertThat(response.priorityContents()).hasSize(10);
    }
}
