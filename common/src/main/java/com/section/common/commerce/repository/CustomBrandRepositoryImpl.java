package com.section.common.commerce.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.commerce.entity.Brand;

import java.util.List;

import static com.section.common.commerce.entity.QBrand.brand;

public class CustomBrandRepositoryImpl implements CustomBrandRepository {

    private final JPAQueryFactory queryFactory;

    public CustomBrandRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<Brand> getBrandList(String keyword, String isActive) {
        return queryFactory
                .selectFrom(brand)
                .where(keywordLike(keyword), isActiveEq(isActive))
                .orderBy(brand.brandNo.desc())
                .fetch();
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return brand.nameKo.containsIgnoreCase(keyword.trim())
                .or(brand.nameEn.containsIgnoreCase(keyword.trim()));
    }

    private BooleanExpression isActiveEq(String isActive) {
        if (isActive == null || isActive.isBlank()) {
            return null;
        }
        return brand.isActive.eq(isActive.trim().toUpperCase());
    }
}
