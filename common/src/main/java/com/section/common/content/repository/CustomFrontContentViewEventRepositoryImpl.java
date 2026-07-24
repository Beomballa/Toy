package com.section.common.content.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.content.dto.ContentViewDataQualityRow;
import com.section.common.content.dto.ContentViewSummaryRow;
import com.section.common.content.dto.ContentViewTopRow;
import com.section.common.content.dto.ContentViewTrendRow;
import com.section.common.content.entity.Document;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import static com.section.common.content.entity.QDocument.document;
import static com.section.common.content.entity.QFrontContentViewEvent.frontContentViewEvent;

@RequiredArgsConstructor
public class CustomFrontContentViewEventRepositoryImpl implements CustomFrontContentViewEventRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public ContentViewDataQualityRow getDataQuality() {
        ContentViewDataQualityRow row = queryFactory
                .select(Projections.constructor(
                        ContentViewDataQualityRow.class,
                        frontContentViewEvent.count(),
                        document.id.count(),
                        frontContentViewEvent.viewedDate.min(),
                        frontContentViewEvent.viewedDate.max()
                ))
                .from(frontContentViewEvent)
                .leftJoin(document).on(document.id.eq(frontContentViewEvent.documentNo))
                .fetchOne();
        return row == null ? new ContentViewDataQualityRow(0, 0, 0, null, null) : row;
    }

    @Override
    public ContentViewSummaryRow getViewSummary(
            LocalDate startDate,
            LocalDate endDate,
            Document.BoardType boardType
    ) {
        JPAQuery<ContentViewSummaryRow> query = queryFactory
                .select(Projections.constructor(
                        ContentViewSummaryRow.class,
                        frontContentViewEvent.count(),
                        frontContentViewEvent.visitorKey.countDistinct(),
                        frontContentViewEvent.documentNo.countDistinct()
                ))
                .from(frontContentViewEvent)
                .join(document).on(document.id.eq(frontContentViewEvent.documentNo));
        ContentViewSummaryRow row = query
                .where(viewedDateBetween(startDate, endDate), boardTypeEq(boardType))
                .fetchOne();
        return row == null ? new ContentViewSummaryRow(0, 0, 0) : row;
    }

    @Override
    public List<ContentViewTrendRow> getDailyViewTrend(
            LocalDate startDate,
            LocalDate endDate,
            Document.BoardType boardType
    ) {
        JPAQuery<ContentViewTrendRow> query = queryFactory
                .select(Projections.constructor(
                        ContentViewTrendRow.class,
                        frontContentViewEvent.viewedDate,
                        frontContentViewEvent.count(),
                        frontContentViewEvent.visitorKey.countDistinct()
                ))
                .from(frontContentViewEvent)
                .join(document).on(document.id.eq(frontContentViewEvent.documentNo));
        return query
                .where(viewedDateBetween(startDate, endDate), boardTypeEq(boardType))
                .groupBy(frontContentViewEvent.viewedDate)
                .orderBy(frontContentViewEvent.viewedDate.asc())
                .fetch();
    }

    @Override
    public List<ContentViewTopRow> getTopViewedContents(
            LocalDate startDate,
            LocalDate endDate,
            Document.BoardType boardType,
            int limit
    ) {
        return queryFactory
                .select(Projections.constructor(
                        ContentViewTopRow.class,
                        document.id,
                        document.boardType,
                        document.title,
                        frontContentViewEvent.count(),
                        frontContentViewEvent.visitorKey.countDistinct()
                ))
                .from(frontContentViewEvent)
                .join(document).on(document.id.eq(frontContentViewEvent.documentNo))
                .where(viewedDateBetween(startDate, endDate), boardTypeEq(boardType))
                .groupBy(document.id, document.boardType, document.title)
                .orderBy(frontContentViewEvent.count().desc(), document.id.desc())
                .limit(Math.max(1, Math.min(limit, 50)))
                .fetch();
    }

    private BooleanExpression viewedDateBetween(LocalDate startDate, LocalDate endDate) {
        return frontContentViewEvent.viewedDate.between(startDate, endDate);
    }

    private BooleanExpression boardTypeEq(Document.BoardType boardType) {
        return boardType == null ? null : document.boardType.eq(boardType);
    }
}
