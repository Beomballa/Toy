package com.section.common.commerce.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.commerce.dto.BannerListQuery;
import com.section.common.commerce.dto.BannerListResDto;

import java.util.List;

import static com.section.common.commerce.entity.QDisplayBanner.displayBanner;

public class CustomBannerRepositoryImpl implements CustomBannerRepository {

    private final JPAQueryFactory queryFactory;

    public CustomBannerRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<BannerListResDto> getBannerList(BannerListQuery query) {
        return queryFactory
                .select(Projections.bean(
                        BannerListResDto.class,
                        displayBanner.bannerNo,
                        displayBanner.title,
                        displayBanner.imageUrl,
                        displayBanner.targetUrl,
                        displayBanner.startDtm,
                        displayBanner.endDtm,
                        displayBanner.sortOrder,
                        displayBanner.isActive
                ))
                .from(displayBanner)
                .where(keywordLike(query.keyword()), isActiveEq(query.isActive()))
                .orderBy(displayBanner.sortOrder.asc(), displayBanner.bannerNo.desc())
                .fetch();
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return displayBanner.title.containsIgnoreCase(keyword.trim());
    }

    private BooleanExpression isActiveEq(String isActive) {
        if (isActive == null || isActive.isBlank()) {
            return null;
        }
        return displayBanner.isActive.eq(isActive.trim().toUpperCase());
    }
}
