package com.section.common.content.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.DocumentDailyStatsRow;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.dto.DocumentSummaryDto;
import com.section.common.content.dto.PopularPublicContentRow;
import com.section.common.content.dto.PublicDocumentRow;
import com.section.common.content.entity.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.section.common.content.entity.QDocument.document;
import static com.section.common.content.entity.QFrontContentViewEvent.frontContentViewEvent;

@RequiredArgsConstructor
public class CustomDocumentRepositoryImpl implements CustomDocumentRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<DocumentListItemDto> getDocumentList(DocumentListQuery query, Pageable pageable) {
        List<DocumentListItemDto> items = queryFactory
                .select(Projections.bean(
                        DocumentListItemDto.class,
                        document.id.as("id"),
                        document.boardType.stringValue().as("boardType"),
                        document.status.stringValue().as("status"),
                        document.publicYn.stringValue().as("publicYn"),
                        document.pinnedYn.stringValue().as("pinnedYn"),
                        document.title.as("title"),
                        document.content.as("contentPreview"),
                        document.viewCnt.as("viewCnt"),
                        document.productNo.as("productNo"),
                        document.crtDtm.as("crtDtm")
                ))
                .from(document)
                .where(
                        boardTypeEq(query.boardType()),
                        keywordLike(query.keyword()),
                        statusEq(query.status()),
                        publicYnEq(query.publicYn()),
                        pinnedOnly(query.pinnedOnly()),
                        productNoEq(query.productNo()),
                        productLinkedEq(query.productLinked()),
                        createdAtBetween(query.startDateTime(), query.endDateTime())
                )
                .orderBy(document.pinnedYn.desc(), document.crtDtm.desc(), document.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(document.count())
                .from(document)
                .where(
                        boardTypeEq(query.boardType()),
                        keywordLike(query.keyword()),
                        statusEq(query.status()),
                        publicYnEq(query.publicYn()),
                        pinnedOnly(query.pinnedOnly()),
                        productNoEq(query.productNo()),
                        productLinkedEq(query.productLinked()),
                        createdAtBetween(query.startDateTime(), query.endDateTime())
                );

        return PageableExecutionUtils.getPage(items, pageable, countQuery::fetchOne);
    }

    @Override
    public DocumentSummaryDto getDocumentSummary(DocumentListQuery query) {
        long totalCount = countBy(query);
        long publishedCount = countBy(query, document.status.eq(Document.PublishStatus.PUBLISHED));
        long draftCount = countBy(query, document.status.eq(Document.PublishStatus.DRAFT));
        long publicCount = countBy(query, document.publicYn.eq(YN.Y));
        long privateCount = countBy(query, document.publicYn.eq(YN.N));
        long pinnedCount = countBy(query, document.pinnedYn.eq(YN.Y));
        long linkedCount = countBy(query, document.productNo.isNotNull());
        long totalViewCount = queryFactory
                .select(document.viewCnt)
                .from(document)
                .where(basePredicates(query))
                .fetch()
                .stream()
                .filter(java.util.Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();

        return new DocumentSummaryDto(
                totalCount,
                publishedCount,
                draftCount,
                publicCount,
                privateCount,
                pinnedCount,
                linkedCount,
                totalViewCount
        );
    }

    @Override
    public List<DocumentDailyStatsRow> getDocumentDailyStats() {
        NumberExpression<Long> publishedCount = countWhen(document.status.eq(Document.PublishStatus.PUBLISHED));
        NumberExpression<Long> draftCount = countWhen(document.status.eq(Document.PublishStatus.DRAFT));
        NumberExpression<Long> publicCount = countWhen(document.publicYn.eq(YN.Y));
        NumberExpression<Long> privateCount = countWhen(document.publicYn.eq(YN.N));
        NumberExpression<Long> pinnedCount = countWhen(document.pinnedYn.eq(YN.Y));
        NumberExpression<Long> linkedCount = countWhen(document.productNo.isNotNull());

        return queryFactory
                .select(Projections.constructor(
                        DocumentDailyStatsRow.class,
                        document.boardType,
                        document.count(),
                        publishedCount,
                        draftCount,
                        publicCount,
                        privateCount,
                        pinnedCount,
                        linkedCount,
                        document.viewCnt.sumLong().coalesce(0L)
                ))
                .from(document)
                .groupBy(document.boardType)
                .orderBy(document.boardType.asc())
                .fetch();
    }

    @Override
    public List<PublicDocumentRow> getPublicDocuments(Document.BoardType boardType, int limit) {
        return queryFactory
                .select(Projections.constructor(
                        PublicDocumentRow.class,
                        document.id,
                        document.boardType,
                        document.title,
                        document.content,
                        document.viewCnt,
                        document.pinnedYn,
                        document.crtDtm
                ))
                .from(document)
                .where(
                        document.boardType.eq(boardType),
                        document.status.eq(Document.PublishStatus.PUBLISHED),
                        document.publicYn.eq(YN.Y)
                )
                .orderBy(document.pinnedYn.desc(), document.crtDtm.desc(), document.id.desc())
                .limit(Math.max(1, Math.min(limit, 8)))
                .fetch();
    }

    @Override
    public List<PopularPublicContentRow> getPopularPublicDocuments(
            LocalDate startDate,
            LocalDate endDate,
            int limit
    ) {
        return queryFactory
                .select(Projections.constructor(
                        PopularPublicContentRow.class,
                        document.id,
                        document.boardType,
                        document.title,
                        document.content,
                        frontContentViewEvent.count(),
                        frontContentViewEvent.visitorKey.countDistinct(),
                        document.pinnedYn,
                        document.crtDtm
                ))
                .from(frontContentViewEvent)
                .join(document).on(document.id.eq(frontContentViewEvent.documentNo))
                .where(
                        frontContentViewEvent.viewedDate.between(startDate, endDate),
                        document.status.eq(Document.PublishStatus.PUBLISHED),
                        document.publicYn.eq(YN.Y)
                )
                .groupBy(
                        document.id,
                        document.boardType,
                        document.title,
                        document.content,
                        document.pinnedYn,
                        document.crtDtm
                )
                .orderBy(
                        frontContentViewEvent.count().desc(),
                        frontContentViewEvent.visitorKey.countDistinct().desc(),
                        document.crtDtm.desc(),
                        document.id.desc()
                )
                .limit(Math.max(1, Math.min(limit, 8)))
                .fetch();
    }

    @Override
    public Optional<PublicDocumentRow> getPublicDocument(long documentId) {
        PublicDocumentRow row = queryFactory
                .select(Projections.constructor(
                        PublicDocumentRow.class,
                        document.id,
                        document.boardType,
                        document.title,
                        document.content,
                        document.viewCnt,
                        document.pinnedYn,
                        document.crtDtm
                ))
                .from(document)
                .where(
                        document.id.eq(documentId),
                        document.status.eq(Document.PublishStatus.PUBLISHED),
                        document.publicYn.eq(YN.Y)
                )
                .fetchOne();
        return Optional.ofNullable(row);
    }

    private NumberExpression<Long> countWhen(BooleanExpression predicate) {
        return new CaseBuilder()
                .when(predicate)
                .then(1L)
                .otherwise(0L)
                .sumLong();
    }

    private BooleanExpression boardTypeEq(Document.BoardType boardType) {
        if (boardType == null) {
            return null;
        }
        return document.boardType.eq(boardType);
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        List<String> terms = Arrays.stream(keyword.trim().split("\\s+"))
                .filter(term -> !term.isBlank())
                .toList();

        BooleanExpression predicate = null;
        for (String term : terms) {
            BooleanExpression termPredicate = document.title.containsIgnoreCase(term)
                    .or(document.content.containsIgnoreCase(term));
            predicate = predicate == null ? termPredicate : predicate.and(termPredicate);
        }
        return predicate;
    }

    private BooleanExpression statusEq(Document.PublishStatus status) {
        if (status == null) {
            return null;
        }
        return document.status.eq(status);
    }

    private BooleanExpression publicYnEq(YN publicYn) {
        if (publicYn == null) {
            return null;
        }
        return document.publicYn.eq(publicYn);
    }

    private BooleanExpression pinnedOnly(Boolean pinnedOnly) {
        if (!Boolean.TRUE.equals(pinnedOnly)) {
            return null;
        }
        return document.pinnedYn.eq(YN.Y);
    }

    private BooleanExpression productNoEq(Long productNo) {
        if (productNo == null) {
            return null;
        }
        return document.productNo.eq(productNo);
    }

    private BooleanExpression productLinkedEq(Boolean productLinked) {
        if (productLinked == null) {
            return null;
        }
        return productLinked ? document.productNo.isNotNull() : document.productNo.isNull();
    }

    private BooleanExpression createdAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null && endDateTime == null) {
            return null;
        }
        if (startDateTime != null && endDateTime != null) {
            return document.crtDtm.between(startDateTime, endDateTime);
        }
        if (startDateTime != null) {
            return document.crtDtm.goe(startDateTime);
        }
        return document.crtDtm.loe(endDateTime);
    }

    private long countBy(DocumentListQuery query, BooleanExpression... extraPredicates) {
        Long count = queryFactory
                .select(document.count())
                .from(document)
                .where(mergePredicates(query, extraPredicates))
                .fetchOne();
        return count == null ? 0L : count;
    }

    private BooleanExpression[] basePredicates(DocumentListQuery query) {
        return new BooleanExpression[] {
                boardTypeEq(query.boardType()),
                keywordLike(query.keyword()),
                statusEq(query.status()),
                publicYnEq(query.publicYn()),
                pinnedOnly(query.pinnedOnly()),
                productNoEq(query.productNo()),
                productLinkedEq(query.productLinked()),
                createdAtBetween(query.startDateTime(), query.endDateTime())
        };
    }

    private BooleanExpression[] mergePredicates(DocumentListQuery query, BooleanExpression... extraPredicates) {
        BooleanExpression[] basePredicates = basePredicates(query);
        BooleanExpression[] merged = Arrays.copyOf(basePredicates, basePredicates.length + extraPredicates.length);
        System.arraycopy(extraPredicates, 0, merged, basePredicates.length, extraPredicates.length);
        return merged;
    }
}
