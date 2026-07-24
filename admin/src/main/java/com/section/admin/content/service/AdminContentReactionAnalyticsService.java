package com.section.admin.content.service;

import com.section.admin.content.res.ContentReactionAnalyticsResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.dto.ContentReactionAnalyticsSummaryRow;
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

    private int helpfulRate(long helpfulCount, long totalCount) {
        return totalCount == 0 ? 0 : (int) Math.round((double) helpfulCount / totalCount * 100);
    }
}
