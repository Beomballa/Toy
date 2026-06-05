package com.section.common.commerce.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.commerce.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.section.common.commerce.entity.QCategory.category;

public class CustomCategoryRepositoryImpl implements CustomCategoryRepository {

    private final JPAQueryFactory queryFactory;

    public CustomCategoryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Category> getCategoryList(Integer depth, String keyword, String isActive, Pageable pageable) {
        List<Category> content = queryFactory
                .selectFrom(category)
                .where(depthEq(depth), keywordLike(keyword), isActiveEq(isActive))
                .orderBy(category.categoryNo.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(category.count())
                .from(category)
                .where(depthEq(depth), keywordLike(keyword), isActiveEq(isActive))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public List<Category> getSubCategoryList(Long parentNo) {
        return queryFactory
                .selectFrom(category)
                .where(parentNoEq(parentNo))
                .orderBy(category.categoryNo.desc())
                .fetch();
    }

    @Override
    public List<Category> getChildCategories(List<Long> parentNos) {
        if (parentNos == null || parentNos.isEmpty()) {
            return List.of();
        }
        return queryFactory
                .selectFrom(category)
                .where(parentNoIn(parentNos))
                .orderBy(category.parentNo.asc(), category.categoryNo.asc())
                .fetch();
    }

    private BooleanExpression depthEq(Integer depth) {
        return depth == null ? null : category.depth.eq(depth);
    }

    private BooleanExpression parentNoEq(Long parentNo) {
        return parentNo == null ? null : category.parentNo.eq(parentNo);
    }

    private BooleanExpression parentNoIn(List<Long> parentNos) {
        return parentNos == null || parentNos.isEmpty() ? null : category.parentNo.in(parentNos);
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return category.name.containsIgnoreCase(keyword.trim());
    }

    private BooleanExpression isActiveEq(String isActive) {
        if (isActive == null || isActive.isBlank()) {
            return null;
        }
        return category.isActive.eq(isActive.trim().toUpperCase());
    }
}
