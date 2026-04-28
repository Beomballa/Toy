package com.section.common.content.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.entity.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

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
                        document.title.as("title"),
                        document.content.as("contentPreview"),
                        document.viewCnt.as("viewCnt"),
                        document.crtDtm.as("crtDtm")
                ))
                .from(document)
                .where(
                        boardTypeEq(query.boardType()),
                        keywordLike(query.keyword())
                )
                .orderBy(document.crtDtm.desc(), document.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(document.count())
                .from(document)
                .where(
                        boardTypeEq(query.boardType()),
                        keywordLike(query.keyword())
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
}
