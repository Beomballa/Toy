package com.section.common.content.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.content.dto.ContentReactionAnalyticsSummaryRow;
import com.section.common.content.dto.ContentReactionBaseCountRow;
import com.section.common.content.dto.ContentReactionCountRow;
import com.section.common.content.dto.ContentReactionDailyTypeCountRow;
import com.section.common.content.dto.ContentReactionDocumentTotalRow;
import com.section.common.content.dto.ContentReactionDocumentTypeCountRow;
import com.section.common.content.dto.ContentReactionSummaryRow;
import com.section.common.content.dto.ContentReactionTopRow;
import com.section.common.content.dto.ContentReactionTrendRow;
import com.section.common.content.entity.Document;
import com.section.common.content.entity.FrontContentReaction;
import lombok.RequiredArgsConstructor;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.section.common.content.entity.QDocument.document;
import static com.section.common.content.entity.QFrontContentReaction.frontContentReaction;

@RequiredArgsConstructor
public class CustomFrontContentReactionRepositoryImpl implements CustomFrontContentReactionRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public ContentReactionSummaryRow getSummary(long documentNo) {
        List<ContentReactionCountRow> rows = queryFactory
                .select(Projections.constructor(
                        ContentReactionCountRow.class,
                        frontContentReaction.reactionType,
                        frontContentReaction.count()
                ))
                .from(frontContentReaction)
                .where(frontContentReaction.documentNo.eq(documentNo))
                .groupBy(frontContentReaction.reactionType)
                .fetch();
        long helpfulCount = countOf(rows, FrontContentReaction.ReactionType.HELPFUL);
        long notHelpfulCount = countOf(rows, FrontContentReaction.ReactionType.NOT_HELPFUL);
        return new ContentReactionSummaryRow(helpfulCount, notHelpfulCount);
    }

    @Override
    public ContentReactionAnalyticsSummaryRow getAnalyticsSummary(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive,
            Document.BoardType boardType
    ) {
        BooleanBuilder conditions = analyticsConditions(startInclusive, endExclusive, boardType);
        ContentReactionBaseCountRow base = queryFactory
                .select(Projections.constructor(
                        ContentReactionBaseCountRow.class,
                        frontContentReaction.count(),
                        frontContentReaction.visitorKey.countDistinct(),
                        frontContentReaction.documentNo.countDistinct()
                ))
                .from(frontContentReaction)
                .join(document).on(document.id.eq(frontContentReaction.documentNo))
                .where(conditions)
                .fetchOne();
        List<ContentReactionCountRow> typeCounts = queryFactory
                .select(Projections.constructor(
                        ContentReactionCountRow.class,
                        frontContentReaction.reactionType,
                        frontContentReaction.count()
                ))
                .from(frontContentReaction)
                .join(document).on(document.id.eq(frontContentReaction.documentNo))
                .where(conditions)
                .groupBy(frontContentReaction.reactionType)
                .fetch();
        ContentReactionBaseCountRow safeBase = base == null
                ? new ContentReactionBaseCountRow(0, 0, 0)
                : base;
        return new ContentReactionAnalyticsSummaryRow(
                safeBase.totalCount(),
                countOf(typeCounts, FrontContentReaction.ReactionType.HELPFUL),
                countOf(typeCounts, FrontContentReaction.ReactionType.NOT_HELPFUL),
                safeBase.uniqueVisitors(),
                safeBase.evaluatedContentCount()
        );
    }

    @Override
    public List<ContentReactionTrendRow> getDailyReactionTrend(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive,
            Document.BoardType boardType
    ) {
        DateExpression<Date> reactedDate = Expressions.dateTemplate(
                Date.class,
                "DATE({0})",
                frontContentReaction.updatedDtm
        );
        List<ContentReactionDailyTypeCountRow> rows = queryFactory
                .select(Projections.constructor(
                        ContentReactionDailyTypeCountRow.class,
                        reactedDate,
                        frontContentReaction.reactionType,
                        frontContentReaction.count()
                ))
                .from(frontContentReaction)
                .join(document).on(document.id.eq(frontContentReaction.documentNo))
                .where(analyticsConditions(startInclusive, endExclusive, boardType))
                .groupBy(reactedDate, frontContentReaction.reactionType)
                .orderBy(reactedDate.asc())
                .fetch();
        return rows.stream()
                .collect(Collectors.groupingBy(
                        row -> row.reactedDate().toLocalDate(),
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> new ContentReactionTrendRow(
                        entry.getKey(),
                        dailyCountOf(entry.getValue(), FrontContentReaction.ReactionType.HELPFUL),
                        dailyCountOf(entry.getValue(), FrontContentReaction.ReactionType.NOT_HELPFUL)
                ))
                .sorted(Comparator.comparing(ContentReactionTrendRow::reactedDate))
                .toList();
    }

    @Override
    public List<ContentReactionTopRow> getTopReactedContents(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive,
            Document.BoardType boardType,
            int limit
    ) {
        List<ContentReactionDocumentTotalRow> totals = queryFactory
                .select(Projections.constructor(
                        ContentReactionDocumentTotalRow.class,
                        document.id,
                        document.boardType,
                        document.title,
                        frontContentReaction.count()
                ))
                .from(frontContentReaction)
                .join(document).on(document.id.eq(frontContentReaction.documentNo))
                .where(analyticsConditions(startInclusive, endExclusive, boardType))
                .groupBy(document.id, document.boardType, document.title)
                .orderBy(frontContentReaction.count().desc(), document.id.desc())
                .limit(Math.max(1, limit))
                .fetch();
        if (totals.isEmpty()) {
            return List.of();
        }

        List<Long> documentIds = totals.stream().map(ContentReactionDocumentTotalRow::documentId).toList();
        Map<Long, List<ContentReactionDocumentTypeCountRow>> countsByDocument = queryFactory
                .select(Projections.constructor(
                        ContentReactionDocumentTypeCountRow.class,
                        frontContentReaction.documentNo,
                        frontContentReaction.reactionType,
                        frontContentReaction.count()
                ))
                .from(frontContentReaction)
                .where(
                        frontContentReaction.documentNo.in(documentIds),
                        frontContentReaction.updatedDtm.goe(startInclusive),
                        frontContentReaction.updatedDtm.lt(endExclusive)
                )
                .groupBy(frontContentReaction.documentNo, frontContentReaction.reactionType)
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(ContentReactionDocumentTypeCountRow::documentId));

        return totals.stream()
                .map(total -> {
                    List<ContentReactionDocumentTypeCountRow> counts =
                            countsByDocument.getOrDefault(total.documentId(), List.of());
                    return new ContentReactionTopRow(
                            total.documentId(),
                            total.boardType(),
                            total.title(),
                            documentCountOf(counts, FrontContentReaction.ReactionType.HELPFUL),
                            documentCountOf(counts, FrontContentReaction.ReactionType.NOT_HELPFUL)
                    );
                })
                .toList();
    }

    private BooleanBuilder analyticsConditions(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive,
            Document.BoardType boardType
    ) {
        BooleanBuilder builder = new BooleanBuilder()
                .and(frontContentReaction.updatedDtm.goe(startInclusive))
                .and(frontContentReaction.updatedDtm.lt(endExclusive));
        if (boardType != null) {
            builder.and(document.boardType.eq(boardType));
        }
        return builder;
    }

    private long countOf(
            List<ContentReactionCountRow> rows,
            FrontContentReaction.ReactionType reactionType
    ) {
        return rows.stream()
                .filter(row -> row.reactionType() == reactionType)
                .mapToLong(ContentReactionCountRow::count)
                .findFirst()
                .orElse(0);
    }

    private long dailyCountOf(
            List<ContentReactionDailyTypeCountRow> rows,
            FrontContentReaction.ReactionType reactionType
    ) {
        return rows.stream()
                .filter(row -> row.reactionType() == reactionType)
                .mapToLong(ContentReactionDailyTypeCountRow::count)
                .findFirst()
                .orElse(0);
    }

    private long documentCountOf(
            List<ContentReactionDocumentTypeCountRow> rows,
            FrontContentReaction.ReactionType reactionType
    ) {
        return rows.stream()
                .filter(row -> row.reactionType() == reactionType)
                .mapToLong(ContentReactionDocumentTypeCountRow::count)
                .findFirst()
                .orElse(0);
    }
}
