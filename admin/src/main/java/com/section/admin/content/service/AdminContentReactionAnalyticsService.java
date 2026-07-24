package com.section.admin.content.service;

import com.section.admin.content.res.ContentReactionAnalyticsResponse;
import com.section.admin.content.res.ContentReactionDataQualityResponse;
import com.section.admin.content.res.ContentReactionDetailResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.dto.ContentReactionAnalyticsSummaryRow;
import com.section.common.content.dto.ContentReactionDataQualityRow;
import com.section.common.content.dto.ContentReactionSummaryRow;
import com.section.common.content.dto.ContentReactionTopRow;
import com.section.common.content.dto.ContentReactionTrendRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.FrontContentReactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class AdminContentReactionAnalyticsService {

    private static final Set<Integer> SUPPORTED_RANGE_DAYS = Set.of(7, 14, 30);
    private static final Set<Integer> SUPPORTED_DETAIL_RANGE_DAYS = Set.of(7, 30, 90);
    private static final int CANDIDATE_LIMIT = 50;
    private static final int DISPLAY_LIMIT = 5;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String METRIC_BASIS = "기간 내 마지막 선택 시각 기준 현재 반응";

    private final FrontContentReactionRepository reactionRepository;
    private final Clock clock;

    @Autowired
    public AdminContentReactionAnalyticsService(FrontContentReactionRepository reactionRepository) {
        this(reactionRepository, Clock.systemDefaultZone());
    }

    AdminContentReactionAnalyticsService(FrontContentReactionRepository reactionRepository, Clock clock) {
        this.reactionRepository = reactionRepository;
        this.clock = clock;
    }

    public ContentReactionAnalyticsResponse getAnalytics(Document.BoardType boardType, int rangeDays) {
        if (!SUPPORTED_RANGE_DAYS.contains(rangeDays)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        LocalDate endDate = LocalDate.now(clock);
        LocalDate startDate = endDate.minusDays(rangeDays - 1L);
        LocalDateTime startInclusive = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        ContentReactionAnalyticsSummaryRow summary =
                reactionRepository.getAnalyticsSummary(startInclusive, endExclusive, boardType);
        List<ContentReactionTrendRow> trend =
                reactionRepository.getDailyReactionTrend(startInclusive, endExclusive, boardType);
        List<ContentReactionTopRow> candidates =
                reactionRepository.getTopReactedContents(startInclusive, endExclusive, boardType, CANDIDATE_LIMIT);

        return new ContentReactionAnalyticsResponse(
                boardType == null ? "ALL" : boardType.name(),
                rangeDays,
                startDate.toString(),
                endDate.toString(),
                LocalDateTime.now(clock).format(DATE_TIME_FORMATTER),
                METRIC_BASIS,
                toSummary(summary),
                fillMissingDates(startDate, rangeDays, trend),
                candidates.stream()
                        .limit(DISPLAY_LIMIT)
                        .map(this::toContent)
                        .toList(),
                candidates.stream()
                        .filter(row -> row.notHelpfulCount() > 0)
                        .sorted(Comparator
                                .comparingLong(ContentReactionTopRow::notHelpfulCount).reversed()
                                .thenComparingInt(row -> helpfulRate(row.helpfulCount(), row.totalCount()))
                                .thenComparing(Comparator.comparingLong(ContentReactionTopRow::totalCount).reversed()))
                        .limit(DISPLAY_LIMIT)
                        .map(this::toContent)
                        .toList()
        );
    }

    public ContentReactionDataQualityResponse getDataQuality() {
        ContentReactionDataQualityRow row = reactionRepository.getDataQuality();
        return new ContentReactionDataQualityResponse(
                row.totalCount(),
                row.validCount(),
                row.orphanCount(),
                formatDateTime(row.oldestReactedAt()),
                formatDateTime(row.latestReactedAt()),
                row.orphanCount() == 0 ? "HEALTHY" : "CLEANUP_REQUIRED",
                LocalDateTime.now(clock).format(DATE_TIME_FORMATTER)
        );
    }

    public ContentReactionDetailResponse getDocumentInsight(long documentId, int rangeDays) {
        if (!SUPPORTED_DETAIL_RANGE_DAYS.contains(rangeDays)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        LocalDate endDate = LocalDate.now(clock);
        LocalDate startDate = endDate.minusDays(rangeDays - 1L);
        ContentReactionSummaryRow summary = reactionRepository.getSummary(documentId);
        List<ContentReactionTrendRow> rows = reactionRepository.getDailyReactionTrend(
                documentId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );
        long recentActivityCount = rows.stream().mapToLong(ContentReactionTrendRow::totalCount).sum();
        long totalCount = summary.helpfulCount() + summary.notHelpfulCount();
        int rate = helpfulRate(summary.helpfulCount(), totalCount);
        String status = insightStatus(totalCount, rate);

        return new ContentReactionDetailResponse(
                documentId,
                rangeDays,
                startDate.toString(),
                endDate.toString(),
                totalCount,
                summary.helpfulCount(),
                summary.notHelpfulCount(),
                rate,
                recentActivityCount,
                status,
                insightStatusMessage(status),
                fillDocumentTrend(startDate, rangeDays, rows)
        );
    }

    private ContentReactionAnalyticsResponse.Summary toSummary(ContentReactionAnalyticsSummaryRow row) {
        return new ContentReactionAnalyticsResponse.Summary(
                row.totalCount(),
                row.helpfulCount(),
                row.notHelpfulCount(),
                helpfulRate(row.helpfulCount(), row.totalCount()),
                row.uniqueVisitors(),
                row.evaluatedContentCount()
        );
    }

    private List<ContentReactionAnalyticsResponse.Trend> fillMissingDates(
            LocalDate startDate,
            int rangeDays,
            List<ContentReactionTrendRow> rows
    ) {
        Map<LocalDate, ContentReactionTrendRow> rowsByDate = rows.stream()
                .collect(Collectors.toMap(ContentReactionTrendRow::reactedDate, Function.identity()));
        return IntStream.range(0, rangeDays)
                .mapToObj(startDate::plusDays)
                .map(date -> {
                    ContentReactionTrendRow row = rowsByDate.get(date);
                    long helpful = row == null ? 0 : row.helpfulCount();
                    long notHelpful = row == null ? 0 : row.notHelpfulCount();
                    long total = helpful + notHelpful;
                    return new ContentReactionAnalyticsResponse.Trend(
                            date.toString(),
                            total,
                            helpful,
                            notHelpful,
                            helpfulRate(helpful, total)
                    );
                })
                .toList();
    }

    private ContentReactionAnalyticsResponse.Content toContent(ContentReactionTopRow row) {
        return new ContentReactionAnalyticsResponse.Content(
                row.documentId(),
                row.boardType().name(),
                row.title(),
                row.totalCount(),
                row.helpfulCount(),
                row.notHelpfulCount(),
                helpfulRate(row.helpfulCount(), row.totalCount())
        );
    }

    private List<ContentReactionDetailResponse.Trend> fillDocumentTrend(
            LocalDate startDate,
            int rangeDays,
            List<ContentReactionTrendRow> rows
    ) {
        Map<LocalDate, ContentReactionTrendRow> rowsByDate = rows.stream()
                .collect(Collectors.toMap(ContentReactionTrendRow::reactedDate, Function.identity()));
        return IntStream.range(0, rangeDays)
                .mapToObj(startDate::plusDays)
                .map(date -> {
                    ContentReactionTrendRow row = rowsByDate.get(date);
                    long helpful = row == null ? 0 : row.helpfulCount();
                    long notHelpful = row == null ? 0 : row.notHelpfulCount();
                    return new ContentReactionDetailResponse.Trend(
                            date.toString(),
                            helpful + notHelpful,
                            helpful,
                            notHelpful
                    );
                })
                .toList();
    }

    private String insightStatus(long totalCount, int helpfulRate) {
        if (totalCount == 0) {
            return "NO_FEEDBACK";
        }
        return totalCount >= 3 && helpfulRate < 60 ? "IMPROVEMENT_REQUIRED" : "HEALTHY";
    }

    private String insightStatusMessage(String status) {
        return switch (status) {
            case "NO_FEEDBACK" -> "아직 독자 반응이 없습니다.";
            case "IMPROVEMENT_REQUIRED" -> "도움 비율이 낮아 제목이나 본문 보완을 검토해 주세요.";
            default -> "현재 도움 비율이 안정적인 콘텐츠입니다.";
        };
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private int helpfulRate(long helpfulCount, long totalCount) {
        return totalCount == 0 ? 0 : (int) Math.round((double) helpfulCount / totalCount * 100);
    }
}
