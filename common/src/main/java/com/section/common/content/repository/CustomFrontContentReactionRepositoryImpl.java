package com.section.common.content.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.content.dto.ContentReactionCountRow;
import com.section.common.content.dto.ContentReactionSummaryRow;
import com.section.common.content.entity.FrontContentReaction;
import lombok.RequiredArgsConstructor;

import java.util.List;

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
}
