package com.section.admin.content.service;

import com.section.admin.content.res.ContentViewDataQualityResponse;
import com.section.admin.content.res.ContentViewAnalyticsResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.dto.ContentViewDataQualityRow;
import com.section.common.content.dto.ContentViewSummaryRow;
import com.section.common.content.dto.ContentViewTopRow;
import com.section.common.content.dto.ContentViewTrendRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.FrontContentViewEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class AdminContentViewAnalyticsService {

    private static final Set<Integer> SUPPORTED_RANGE_DAYS = Set.of(7, 14, 30);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FrontContentViewEventRepository viewEventRepository;
    private final Clock clock;

    @Autowired
    public AdminContentViewAnalyticsService(FrontContentViewEventRepository viewEventRepository) {
        this(viewEventRepository, Clock.systemDefaultZone());
    }

    AdminContentViewAnalyticsService(FrontContentViewEventRepository viewEventRepository, Clock clock) {
        this.viewEventRepository = viewEventRepository;
        this.clock = clock;
    }

    public ContentViewAnalyticsResponse getAnalytics(Document.BoardType boardType, int rangeDays) {
        if (!SUPPORTED_RANGE_DAYS.contains(rangeDays)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        LocalDate endDate = LocalDate.now(clock);
        LocalDate startDate = endDate.minusDays(rangeDays - 1L);
        LocalDate previousEndDate = startDate.minusDays(1);
        LocalDate previousStartDate = previousEndDate.minusDays(rangeDays - 1L);
        ContentViewSummaryRow summary = viewEventRepository.getViewSummary(startDate, endDate, boardType);
        ContentViewSummaryRow previous = viewEventRepository.getViewSummary(previousStartDate, previousEndDate, boardType);
        List<ContentViewTrendRow> trendRows = viewEventRepository.getDailyViewTrend(startDate, endDate, boardType);
        List<ContentViewTopRow> topRows = viewEventRepository.getTopViewedContents(startDate, endDate, boardType, 5);

        return new ContentViewAnalyticsResponse(
                boardType == null ? "ALL" : boardType.name(),
                rangeDays,
                startDate.toString(),
                endDate.toString(),
                LocalDateTime.now(clock).format(DATE_TIME_FORMATTER),
                toSummary(summary, previous),
                fillMissingDates(startDate, rangeDays, trendRows),
                topRows.stream().map(this::toTopContent).toList()
        );
    }

    public ContentViewDataQualityResponse getDataQuality() {
        ContentViewDataQualityRow row = viewEventRepository.getDataQuality();
        return new ContentViewDataQualityResponse(
                row.totalEventCount(),
                row.validEventCount(),
                row.orphanEventCount(),
                row.oldestViewedDate() == null ? null : row.oldestViewedDate().toString(),
                row.latestViewedDate() == null ? null : row.latestViewedDate().toString(),
                row.orphanEventCount() == 0 ? "HEALTHY" : "CLEANUP_REQUIRED",
                LocalDateTime.now(clock).format(DATE_TIME_FORMATTER)
        );
    }

    private ContentViewAnalyticsResponse.Summary toSummary(
            ContentViewSummaryRow current,
            ContentViewSummaryRow previous
    ) {
        double average = current.viewedContentCount() == 0
                ? 0
                : Math.round((double) current.totalViews() / current.viewedContentCount() * 10) / 10.0;
        return new ContentViewAnalyticsResponse.Summary(
                current.totalViews(),
                current.uniqueVisitors(),
                current.viewedContentCount(),
                average,
                previous.totalViews(),
                calculateChangeRate(current.totalViews(), previous.totalViews())
        );
    }

    private int calculateChangeRate(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0 : 100;
        }
        return (int) Math.round((double) (current - previous) / previous * 100);
    }

    private List<ContentViewAnalyticsResponse.Trend> fillMissingDates(
            LocalDate startDate,
            int rangeDays,
            List<ContentViewTrendRow> rows
    ) {
        Map<LocalDate, ContentViewTrendRow> rowsByDate = rows.stream()
                .collect(Collectors.toMap(ContentViewTrendRow::viewedDate, Function.identity()));
        return IntStream.range(0, rangeDays)
                .mapToObj(startDate::plusDays)
                .map(date -> {
                    ContentViewTrendRow row = rowsByDate.get(date);
                    return new ContentViewAnalyticsResponse.Trend(
                            date.toString(),
                            row == null ? 0 : row.viewCount(),
                            row == null ? 0 : row.uniqueVisitors()
                    );
                })
                .toList();
    }

    private ContentViewAnalyticsResponse.TopContent toTopContent(ContentViewTopRow row) {
        return new ContentViewAnalyticsResponse.TopContent(
                row.documentId(),
                row.boardType().name(),
                row.title(),
                row.viewCount(),
                row.uniqueVisitors()
        );
    }
}
