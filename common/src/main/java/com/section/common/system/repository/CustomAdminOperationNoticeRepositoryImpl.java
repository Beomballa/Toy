package com.section.common.system.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.system.dto.AdminOperationNoticeListQuery;
import com.section.common.system.dto.AdminOperationNoticeListResDto;
import com.section.common.system.entity.AdminOperationNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static com.section.common.system.entity.QAdminOperationNotice.adminOperationNotice;

public class CustomAdminOperationNoticeRepositoryImpl implements CustomAdminOperationNoticeRepository {

    private final JPAQueryFactory queryFactory;

    public CustomAdminOperationNoticeRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<AdminOperationNoticeListResDto> getNoticeList(AdminOperationNoticeListQuery query, Pageable pageable) {
        List<AdminOperationNoticeListResDto> content = queryFactory
                .select(Projections.bean(
                        AdminOperationNoticeListResDto.class,
                        adminOperationNotice.noticeNo,
                        adminOperationNotice.title,
                        adminOperationNotice.content,
                        adminOperationNotice.isActive,
                        adminOperationNotice.isPinned,
                        adminOperationNotice.startDtm,
                        adminOperationNotice.endDtm,
                        adminOperationNotice.crtDtm
                ))
                .from(adminOperationNotice)
                .where(
                        keywordLike(query.keyword()),
                        isActiveEq(query.isActive()),
                        isPinnedEq(query.isPinned())
                )
                .orderBy(
                        adminOperationNotice.isPinned.desc(),
                        adminOperationNotice.noticeNo.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(adminOperationNotice.count())
                .from(adminOperationNotice)
                .where(
                        keywordLike(query.keyword()),
                        isActiveEq(query.isActive()),
                        isPinnedEq(query.isPinned())
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public List<AdminOperationNotice> getActiveDashboardNotices(LocalDateTime now, int limit) {
        return queryFactory
                .selectFrom(adminOperationNotice)
                .where(
                        adminOperationNotice.isActive.eq("Y"),
                        startedAtOrBefore(now),
                        endsAtOrAfter(now)
                )
                .orderBy(
                        adminOperationNotice.isPinned.desc(),
                        adminOperationNotice.noticeNo.desc()
                )
                .limit(limit)
                .fetch();
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return adminOperationNotice.title.containsIgnoreCase(keyword.trim())
                .or(adminOperationNotice.content.containsIgnoreCase(keyword.trim()));
    }

    private BooleanExpression isActiveEq(String isActive) {
        if (isActive == null || isActive.isBlank()) {
            return null;
        }
        return adminOperationNotice.isActive.eq(isActive.trim().toUpperCase());
    }

    private BooleanExpression isPinnedEq(String isPinned) {
        if (isPinned == null || isPinned.isBlank()) {
            return null;
        }
        return adminOperationNotice.isPinned.eq(isPinned.trim().toUpperCase());
    }

    private BooleanExpression startedAtOrBefore(LocalDateTime now) {
        return adminOperationNotice.startDtm.isNull()
                .or(adminOperationNotice.startDtm.loe(now));
    }

    // 종료 시각이 비어 있으면 수동 중지 전까지 계속 노출한다.
    private BooleanExpression endsAtOrAfter(LocalDateTime now) {
        return adminOperationNotice.endDtm.isNull()
                .or(adminOperationNotice.endDtm.goe(now));
    }
}
