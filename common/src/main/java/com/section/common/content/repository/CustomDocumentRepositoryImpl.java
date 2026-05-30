package com.section.common.content.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.entity.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.section.common.content.entity.QDocument.document;

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
                        document.crtDtm.as("crtDtm")
                ))
                .from(document)
                .where(
                        boardTypeEq(query.boardType()),
                        keywordLike(query.keyword()),
                        statusEq(query.status()),
                        publicYnEq(query.publicYn()),
                        pinnedOnly(query.pinnedOnly()),
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
                        createdAtBetween(query.startDateTime(), query.endDateTime())
                );

        return PageableExecutionUtils.getPage(items, pageable, countQuery::fetchOne);
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

        String normalizedKeyword = keyword.trim();
        return document.title.containsIgnoreCase(normalizedKeyword)
                .or(document.content.containsIgnoreCase(normalizedKeyword));
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
}
