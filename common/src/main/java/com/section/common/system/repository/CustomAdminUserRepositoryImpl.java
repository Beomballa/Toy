package com.section.common.system.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.system.dto.AdminUserListQuery;
import com.section.common.system.dto.AdminUserListResDto;
import com.section.common.system.dto.AdminUserSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.section.common.system.entity.QAdminUser.adminUser;

public class CustomAdminUserRepositoryImpl implements CustomAdminUserRepository {

    private final JPAQueryFactory queryFactory;

    public CustomAdminUserRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<AdminUserListResDto> getAdminUserList(AdminUserListQuery query, Pageable pageable) {
        BooleanBuilder conditions = buildConditions(query, LocalDateTime.now());

        List<AdminUserListResDto> items = queryFactory
                .select(Projections.bean(
                        AdminUserListResDto.class,
                        adminUser.adminNo,
                        adminUser.loginId,
                        adminUser.name,
                        adminUser.role,
                        adminUser.status,
                        adminUser.lastLoginDtm,
                        adminUser.crtDtm
                ))
                .from(adminUser)
                .where(conditions)
                .orderBy(
                        adminUser.role.eq("ROLE_SUPER").desc(),
                        adminUser.status.eq("ACTIVE").desc(),
                        adminUser.lastLoginDtm.desc().nullsLast(),
                        adminUser.adminNo.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(adminUser.count())
                .from(adminUser)
                .where(conditions);

        return PageableExecutionUtils.getPage(items, pageable, countQuery::fetchOne);
    }

    @Override
    public AdminUserSummaryDto getAdminUserSummary(AdminUserListQuery query, LocalDateTime now) {
        BooleanBuilder conditions = buildConditions(query, now);
        LocalDateTime inactiveThreshold = now.minusDays(7);
        NumberExpression<Long> totalCount = adminUser.count();
        NumberExpression<Long> activeCount = new CaseBuilder().when(adminUser.status.eq("ACTIVE")).then(1L).otherwise(0L).sum();
        NumberExpression<Long> suspendedCount = new CaseBuilder().when(adminUser.status.eq("SUSPENDED")).then(1L).otherwise(0L).sum();
        NumberExpression<Long> superCount = new CaseBuilder().when(adminUser.role.eq("ROLE_SUPER")).then(1L).otherwise(0L).sum();
        NumberExpression<Long> inactiveCount = new CaseBuilder()
                .when(adminUser.lastLoginDtm.isNull()
                        .or(adminUser.lastLoginDtm.before(inactiveThreshold)))
                .then(1L)
                .otherwise(0L)
                .sum();

        Tuple tuple = queryFactory
                .select(
                        totalCount,
                        activeCount,
                        suspendedCount,
                        superCount,
                        inactiveCount
                )
                .from(adminUser)
                .where(conditions)
                .fetchOne();

        if (tuple == null) {
            return new AdminUserSummaryDto(0, 0, 0, 0, 0);
        }

        return new AdminUserSummaryDto(
                safeLong(tuple.get(totalCount)),
                safeLong(tuple.get(activeCount)),
                safeLong(tuple.get(suspendedCount)),
                safeLong(tuple.get(superCount)),
                safeLong(tuple.get(inactiveCount))
        );
    }

    private BooleanBuilder buildConditions(AdminUserListQuery query, LocalDateTime now) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(keywordCondition(query.keyword()));
        builder.and(roleEq(query.role()));
        builder.and(statusEq(query.status()));
        builder.and(inactiveCondition(query.inactiveDays(), now));
        builder.and(neverLoggedInCondition(query.neverLoggedInOnly()));
        return builder;
    }

    private BooleanExpression keywordCondition(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String[] tokens = Arrays.stream(keyword.trim().split("\\s+"))
                .filter(token -> !token.isBlank())
                .toArray(String[]::new);

        BooleanBuilder builder = new BooleanBuilder();
        for (String token : tokens) {
            builder.and(
                    adminUser.loginId.containsIgnoreCase(token)
                            .or(adminUser.name.containsIgnoreCase(token))
            );
        }
        return builder;
    }

    private BooleanExpression roleEq(String role) {
        return role == null ? null : adminUser.role.eq(role);
    }

    private BooleanExpression statusEq(String status) {
        return status == null ? null : adminUser.status.eq(status);
    }

    private BooleanExpression inactiveCondition(Integer inactiveDays, LocalDateTime now) {
        if (inactiveDays == null || inactiveDays <= 0) {
            return null;
        }
        LocalDateTime threshold = now.minusDays(inactiveDays);
        return adminUser.lastLoginDtm.isNull()
                .or(adminUser.lastLoginDtm.before(threshold));
    }

    private BooleanExpression neverLoggedInCondition(Boolean neverLoggedInOnly) {
        return Boolean.TRUE.equals(neverLoggedInOnly) ? adminUser.lastLoginDtm.isNull() : null;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
