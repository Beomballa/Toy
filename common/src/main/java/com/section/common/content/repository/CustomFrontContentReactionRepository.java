package com.section.common.content.repository;

import com.section.common.content.dto.ContentReactionAnalyticsSummaryRow;
import com.section.common.content.dto.ContentReactionDataQualityRow;
import com.section.common.content.dto.ContentReactionSummaryRow;
import com.section.common.content.dto.ContentReactionTopRow;
import com.section.common.content.dto.ContentReactionTrendRow;
import com.section.common.content.entity.Document;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomFrontContentReactionRepository {

    ContentReactionSummaryRow getSummary(long documentNo);

    ContentReactionDataQualityRow getDataQuality();

    ContentReactionAnalyticsSummaryRow getAnalyticsSummary(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive,
            Document.BoardType boardType
    );

    List<ContentReactionTrendRow> getDailyReactionTrend(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive,
            Document.BoardType boardType
    );

    List<ContentReactionTrendRow> getDailyReactionTrend(
            long documentNo,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

    List<ContentReactionTopRow> getTopReactedContents(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive,
            Document.BoardType boardType,
            int limit
    );
}
