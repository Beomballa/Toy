package com.section.common.content.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

public class CustomDocumentRepositoryImpl implements CustomDocumentRepository {
    private final JPAQueryFactory queryFactory;

    public CustomDocumentRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }
}
