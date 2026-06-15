package com.section.common.commerce.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.commerce.dto.BrandSummaryDto;
import com.section.common.commerce.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.section.common.commerce.entity.QBrand.brand;

public class CustomBrandRepositoryImpl implements CustomBrandRepository {

    private final JPAQueryFactory queryFactory;

    public CustomBrandRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Brand> getBrandList(String keyword, String isActive, Pageable pageable) {
        List<Brand> content = queryFactory
                .selectFrom(brand)
                .where(keywordLike(keyword), isActiveEq(isActive))
                .orderBy(brand.brandNo.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(brand.count())
                .from(brand)
                .where(keywordLike(keyword), isActiveEq(isActive))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public BrandSummaryDto getBrandSummary(String keyword) {
        long totalCount = countBy(keyword, null);
        long activeCount = countBy(keyword, "Y");
        long inactiveCount = countBy(keyword, "N");
        return new BrandSummaryDto(totalCount, activeCount, inactiveCount);
    }

    private long countBy(String keyword, String isActive) {
        Long count = queryFactory
                .select(brand.count())
                .from(brand)
                .where(keywordLike(keyword), isActiveEq(isActive))
                .fetchOne();
        return count == null ? 0L : count;
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
