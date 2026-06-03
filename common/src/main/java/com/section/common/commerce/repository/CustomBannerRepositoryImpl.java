package com.section.common.commerce.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.commerce.dto.BannerListQuery;
import com.section.common.commerce.dto.BannerListResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static com.section.common.commerce.entity.QDisplayBanner.displayBanner;

public class CustomBannerRepositoryImpl implements CustomBannerRepository {

    private final JPAQueryFactory queryFactory;

    public CustomBannerRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<BannerListResDto> getBannerList(BannerListQuery query, Pageable pageable) {
        List<BannerListResDto> content = queryFactory
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
                .where(keywordLike(query.keyword()), isActiveEq(query.isActive()), exposureStatusEq(query.exposureStatus()))
                .orderBy(displayBanner.sortOrder.asc(), displayBanner.bannerNo.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(displayBanner.count())
                .from(displayBanner)
                .where(keywordLike(query.keyword()), isActiveEq(query.isActive()), exposureStatusEq(query.exposureStatus()))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public boolean existsActiveBannerScheduleConflict(Long bannerNo, Integer sortOrder, LocalDateTime startDtm, LocalDateTime endDtm) {
        Integer fetched = queryFactory
                .selectOne()
                .from(displayBanner)
                .where(
                        displayBanner.isActive.eq("Y"),
                        displayBanner.sortOrder.eq(sortOrder),
                        bannerNoNe(bannerNo),
                        displayBanner.startDtm.loe(endDtm),
                        displayBanner.endDtm.goe(startDtm)
                )
                .fetchFirst();
        return fetched != null;
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

    private BooleanExpression exposureStatusEq(String exposureStatus) {
        if (exposureStatus == null || exposureStatus.isBlank()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        return switch (exposureStatus.trim().toUpperCase()) {
            case "SCHEDULED" -> displayBanner.startDtm.after(now);
            case "LIVE" -> displayBanner.startDtm.loe(now).and(displayBanner.endDtm.goe(now));
            case "ENDED" -> displayBanner.endDtm.before(now);
            default -> null;
        };
    }

    private BooleanExpression bannerNoNe(Long bannerNo) {
        if (bannerNo == null) {
            return null;
        }
        return displayBanner.bannerNo.ne(bannerNo);
    }
}
