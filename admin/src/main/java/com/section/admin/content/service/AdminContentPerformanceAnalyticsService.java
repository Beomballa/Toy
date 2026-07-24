package com.section.admin.content.service;

import com.section.admin.content.res.ContentPerformanceAnalyticsResponse;
import com.section.admin.task.support.AdminTaskLinkSupport;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.dto.ContentReactionAnalyticsSummaryRow;
import com.section.common.content.dto.ContentReactionTopRow;
import com.section.common.content.dto.ContentViewSummaryRow;
import com.section.common.content.dto.ContentViewTopRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.FrontContentReactionRepository;
import com.section.common.content.repository.FrontContentViewEventRepository;
import com.section.common.system.entity.AdminOperationTask;
import com.section.common.system.repository.AdminOperationTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminContentPerformanceAnalyticsService {

    private static final Set<Integer> SUPPORTED_RANGE_DAYS = Set.of(7, 14, 30);
    private static final int CANDIDATE_LIMIT = 50;
    private static final int DISPLAY_LIMIT = 10;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static final String CONTENT_PERFORMANCE_SOURCE_TYPE = "CONTENT_PERFORMANCE";

    private final FrontContentViewEventRepository viewRepository;
    private final FrontContentReactionRepository reactionRepository;
    private final AdminOperationTaskRepository taskRepository;
    private final Clock clock;

    @Autowired
    public AdminContentPerformanceAnalyticsService(
            FrontContentViewEventRepository viewRepository,
            FrontContentReactionRepository reactionRepository,
            AdminOperationTaskRepository taskRepository
    ) {
        this(viewRepository, reactionRepository, taskRepository, Clock.systemDefaultZone());
    }

    AdminContentPerformanceAnalyticsService(
            FrontContentViewEventRepository viewRepository,
            FrontContentReactionRepository reactionRepository,
            AdminOperationTaskRepository taskRepository,
            Clock clock
    ) {
        this.viewRepository = viewRepository;
        this.reactionRepository = reactionRepository;
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    public ContentPerformanceAnalyticsResponse getAnalytics(Document.BoardType boardType, int rangeDays) {
        if (!SUPPORTED_RANGE_DAYS.contains(rangeDays)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        LocalDate endDate = LocalDate.now(clock);
        LocalDate startDate = endDate.minusDays(rangeDays - 1L);
        ContentViewSummaryRow viewSummary =
                viewRepository.getViewSummary(startDate, endDate, boardType);
        ContentReactionAnalyticsSummaryRow reactionSummary = reactionRepository.getAnalyticsSummary(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                boardType
        );
        List<ContentViewTopRow> viewRows =
                viewRepository.getTopViewedContents(startDate, endDate, boardType, CANDIDATE_LIMIT);
        List<ContentReactionTopRow> reactionRows = reactionRepository.getTopReactedContents(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                boardType,
                CANDIDATE_LIMIT
        );
        List<ContentPerformanceAnalyticsResponse.Content> contents = merge(viewRows, reactionRows);
        List<ContentPerformanceAnalyticsResponse.Content> priorityContents = contents.stream()
                .sorted(Comparator
                        .comparingInt(ContentPerformanceAnalyticsResponse.Content::priorityScore).reversed()
                        .thenComparing(Comparator.comparingLong(
                                ContentPerformanceAnalyticsResponse.Content::viewCount
                        ).reversed())
                        .thenComparing(Comparator.comparingLong(
                                ContentPerformanceAnalyticsResponse.Content::documentId
                        ).reversed()))
                .limit(DISPLAY_LIMIT)
                .toList();
        long actionRequiredCount = priorityContents.stream()
                .filter(this::isActionRequired)
                .count();
        long linkedActionCount = priorityContents.stream()
                .filter(this::isActionRequired)
                .filter(item -> item.operationTaskNo() != null)
                .count();

        return new ContentPerformanceAnalyticsResponse(
                boardType == null ? "ALL" : boardType.name(),
                rangeDays,
                startDate.toString(),
                endDate.toString(),
                LocalDateTime.now(clock).format(DATE_TIME_FORMATTER),
                new ContentPerformanceAnalyticsResponse.Summary(
                        viewSummary.totalViews(),
                        reactionSummary.totalCount(),
                        percentage(reactionSummary.helpfulCount(), reactionSummary.totalCount()),
                        coverageRate(reactionSummary.totalCount(), viewSummary.totalViews()),
                        contents.size(),
                        actionRequiredCount,
                        linkedActionCount,
                        actionRequiredCount - linkedActionCount
                ),
                priorityContents
        );
    }

    private List<ContentPerformanceAnalyticsResponse.Content> merge(
            List<ContentViewTopRow> viewRows,
            List<ContentReactionTopRow> reactionRows
    ) {
        Map<Long, PerformanceCandidate> candidates = new LinkedHashMap<>();
        viewRows.forEach(row -> candidates.computeIfAbsent(
                row.documentId(),
                ignored -> PerformanceCandidate.from(row)
        ).applyView(row));
        reactionRows.forEach(row -> candidates.computeIfAbsent(
                row.documentId(),
                ignored -> PerformanceCandidate.from(row)
        ).applyReaction(row));
        long maxViews = candidates.values().stream().mapToLong(candidate -> candidate.viewCount).max().orElse(0);
        Map<Long, AdminOperationTask> tasksBySourceId = candidates.isEmpty()
                ? Map.of()
                : taskRepository.findAllBySourceTypeAndSourceIdIn(
                                CONTENT_PERFORMANCE_SOURCE_TYPE,
                                candidates.keySet()
                        ).stream()
                        .collect(Collectors.toMap(
                                AdminOperationTask::getSourceId,
                                Function.identity()
                        ));
        return candidates.values().stream()
                .map(candidate -> toContent(candidate, maxViews, tasksBySourceId.get(candidate.documentId)))
                .toList();
    }

    private ContentPerformanceAnalyticsResponse.Content toContent(
            PerformanceCandidate candidate,
            long maxViews,
            AdminOperationTask task
    ) {
        long reactionCount = candidate.helpfulCount + candidate.notHelpfulCount;
        int helpfulRate = percentage(candidate.helpfulCount, reactionCount);
        int coverageRate = coverageRate(reactionCount, candidate.viewCount);
        String status = status(candidate.viewCount, reactionCount, helpfulRate);
        return new ContentPerformanceAnalyticsResponse.Content(
                candidate.documentId,
                candidate.boardType.name(),
                candidate.title,
                candidate.viewCount,
                candidate.uniqueVisitors,
                reactionCount,
                candidate.helpfulCount,
                candidate.notHelpfulCount,
                helpfulRate,
                coverageRate,
                priorityScore(candidate.viewCount, maxViews, reactionCount, helpfulRate, status),
                status,
                statusMessage(status),
                task == null ? null : task.getTaskNo(),
                task == null ? null : buildTaskPath(task.getTaskNo(), candidate.boardType)
        );
    }

    private String buildTaskPath(long taskNo, Document.BoardType boardType) {
        String returnTo = "/admin/content/list?boardType=" + boardType.name();
        return AdminTaskLinkSupport.buildListOpenPath(taskNo, returnTo, "content-performance");
    }

    private boolean isActionRequired(ContentPerformanceAnalyticsResponse.Content content) {
        return "IMPROVEMENT_REQUIRED".equals(content.status()) || "FEEDBACK_NEEDED".equals(content.status());
    }

    private int priorityScore(long views, long maxViews, long reactions, int helpfulRate, String status) {
        int trafficScore = maxViews == 0 ? 0 : (int) Math.round((double) views / maxViews * 50);
        int dissatisfactionScore = reactions == 0 ? 0 : (int) Math.round((100 - helpfulRate) * 0.4);
        int evidenceScore = (int) Math.min(10, reactions);
        int coverageGapScore = "FEEDBACK_NEEDED".equals(status) ? 20 : 0;
        return Math.min(100, trafficScore + dissatisfactionScore + evidenceScore + coverageGapScore);
    }

    private String status(long views, long reactions, int helpfulRate) {
        if (views == 0) {
            return "LOW_SIGNAL";
        }
        if (views >= 5 && reactions == 0) {
            return "FEEDBACK_NEEDED";
        }
        if (reactions >= 3 && helpfulRate < 60) {
            return "IMPROVEMENT_REQUIRED";
        }
        return "HEALTHY";
    }

    private String statusMessage(String status) {
        return switch (status) {
            case "FEEDBACK_NEEDED" -> "조회 대비 반응이 없어 평가 유도나 본문 하단 노출을 점검해 주세요.";
            case "IMPROVEMENT_REQUIRED" -> "조회 유입은 있으나 도움 비율이 낮아 콘텐츠 보완이 필요합니다.";
            case "LOW_SIGNAL" -> "반응은 있으나 기간 내 조회 신호가 적어 추가 관찰이 필요합니다.";
            default -> "조회와 독자 반응이 현재 기준에서 안정적입니다.";
        };
    }

    private int percentage(long numerator, long denominator) {
        return denominator == 0 ? 0 : (int) Math.round((double) numerator / denominator * 100);
    }

    private int coverageRate(long reactions, long views) {
        return views == 0 ? 0 : Math.min(100, percentage(reactions, views));
    }

    private static final class PerformanceCandidate {
        private final long documentId;
        private final Document.BoardType boardType;
        private final String title;
        private long viewCount;
        private long uniqueVisitors;
        private long helpfulCount;
        private long notHelpfulCount;

        private PerformanceCandidate(long documentId, Document.BoardType boardType, String title) {
            this.documentId = documentId;
            this.boardType = boardType;
            this.title = title;
        }

        private static PerformanceCandidate from(ContentViewTopRow row) {
            return new PerformanceCandidate(row.documentId(), row.boardType(), row.title());
        }

        private static PerformanceCandidate from(ContentReactionTopRow row) {
            return new PerformanceCandidate(row.documentId(), row.boardType(), row.title());
        }

        private void applyView(ContentViewTopRow row) {
            viewCount = row.viewCount();
            uniqueVisitors = row.uniqueVisitors();
        }

        private void applyReaction(ContentReactionTopRow row) {
            helpfulCount = row.helpfulCount();
            notHelpfulCount = row.notHelpfulCount();
        }
    }
}
