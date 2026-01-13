package com.section.common.content.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.content.dto.ContentListItemDto;
import com.section.common.content.dto.DocumentListItemDto;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

import static com.section.common.content.entity.QDocument.document;
import static com.section.common.system.entity.QApprovalDocument.approvalDocument;

public class CustomDocumentRepositoryImpl implements CustomDocumentRepository {
    private final JPAQueryFactory queryFactory;

    public CustomDocumentRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<DocumentListItemDto> findDocumentInfo(ContentListItemDto reqDto, Pageable pageable) {
        List<DocumentListItemDto> result = queryFactory
                .select(
                        Projections.bean(
                                DocumentListItemDto.class,
                                document.approvalDocument.docNo.as("docNo"),
                                document.title.as("title"),
                                document.content.as("content"),
                                document.uptDtm.as("uptDtm"),
                                document.viewYn.as("viewYn")
                        )
                )
                .from(document)
                .join(document.approvalDocument, approvalDocument)
                .where(isSearchKeywordCondition(reqDto.getSearchKeyword(), reqDto.getSearchKeywordType()))
                .orderBy(document.reserveDtm.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = Optional.ofNullable(queryFactory
                .select(document.count())
                .from(document)
                .join(document.approvalDocument, approvalDocument)
                .where(isSearchKeywordCondition(reqDto.getSearchKeyword(), reqDto.getSearchKeywordType()))
                .fetchOne()).orElse(0L);

        return new PageImpl<>(result, pageable, total);
    }

    private BooleanExpression isSearchKeywordCondition(String searchKeyword, String searchKeywordType) {
        if(!StringUtils.hasText(searchKeyword) ){
            return null;
        }
        if(searchKeywordType.equals("T")) {
            return StringUtils.hasText(searchKeyword) ? document.title.contains(searchKeyword) : null;
        }else if(searchKeywordType.equals("C")) {
            return StringUtils.hasText(searchKeyword) ? document.title.contains(searchKeyword) : null;
        }
        return StringUtils.hasText(searchKeyword) ? document.title.contains(searchKeyword).or(document.content.contains(searchKeyword))  : null;
    }
}
