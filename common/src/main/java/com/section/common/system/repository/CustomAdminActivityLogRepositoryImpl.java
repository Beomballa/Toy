package com.section.common.system.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.system.dto.AdminActivityLogListQuery;
import com.section.common.system.dto.AdminActivityLogListResDto;
import com.section.common.system.dto.AdminActivityLogSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.section.common.system.entity.QAdminActivityLog.adminActivityLog;

public class CustomAdminActivityLogRepositoryImpl implements CustomAdminActivityLogRepository {

    private final JPAQueryFactory queryFactory;

    public CustomAdminActivityLogRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<AdminActivityLogListResDto> getLogList(AdminActivityLogListQuery query, Pageable pageable) {
        List<AdminActivityLogListResDto> items = queryFactory
                .select(Projections.bean(
                        AdminActivityLogListResDto.class,
                        adminActivityLog.logNo,
                        adminActivityLog.adminNo,
                        adminActivityLog.actionType,
                        adminActivityLog.targetId,
                        adminActivityLog.ipAddress,
                        adminActivityLog.actionDtm
                ))
                .from(adminActivityLog)
                .where(logConditions(query))
                .orderBy(adminActivityLog.logNo.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory.select(adminActivityLog.count())
                .from(adminActivityLog)
                .where(logConditions(query));

        return PageableExecutionUtils.getPage(items, pageable, countQuery::fetchOne);
    }

    @Override
    public AdminActivityLogSummaryDto getLogSummary(AdminActivityLogListQuery query) {
        return queryFactory
                .select(Projections.constructor(
                        AdminActivityLogSummaryDto.class,
                        adminActivityLog.count(),
                        Expressions.numberTemplate(
                                Long.class,
                                "coalesce(sum(case when {0} >= {1} then 1 else 0 end), 0)",
                                adminActivityLog.actionDtm,
                                LocalDate.now().atStartOfDay()
                        ),
                        Expressions.numberTemplate(
                                Long.class,
                                "coalesce(sum(case when upper({0}) like {1} then 1 else 0 end), 0)",
                                adminActivityLog.actionType,
                                "NOTICE_%"
                        ),
                        Expressions.numberTemplate(
                                Long.class,
                                "coalesce(sum(case when upper({0}) like {1} then 1 else 0 end), 0)",
                                adminActivityLog.actionType,
                                "TASK_%"
                        ),
                        Expressions.numberTemplate(
                                Long.class,
                                "coalesce(sum(case when upper({0}) like {1} or upper({0}) like {2} or upper({0}) like {3} or upper({0}) like {4} or upper({0}) like {5} then 1 else 0 end), 0)",
                                adminActivityLog.actionType,
                                "PRODUCT_%",
                                "ORDER_%",
                                "BANNER_%",
                                "BRAND_%",
                                "CATEGORY_%"
                        ),
                        adminActivityLog.adminNo.countDistinct()
                ))
                .from(adminActivityLog)
                .where(logConditions(query))
                .fetchOne();
    }

    private BooleanExpression[] logConditions(AdminActivityLogListQuery query) {
        return new BooleanExpression[]{
                adminNoEq(query.adminNo()),
                actionTypeLike(query.actionType()),
                targetIdEq(query.targetId()),
                actionDateBetween(query.startDate(), query.endDate())
        };
    }

    private BooleanExpression adminNoEq(Long adminNo) {
        return adminNo == null ? null : adminActivityLog.adminNo.eq(adminNo);
    }

    private BooleanExpression actionTypeLike(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return null;
        }
        return adminActivityLog.actionType.containsIgnoreCase(actionType.trim());
    }

    private BooleanExpression targetIdEq(Long targetId) {
        return targetId == null ? null : adminActivityLog.targetId.eq(targetId);
    }

    private BooleanExpression actionDateBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        LocalDateTime startDateTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        if (startDateTime != null && endDateTime != null) {
            return adminActivityLog.actionDtm.between(startDateTime, endDateTime);
        }
        if (startDateTime != null) {
            return adminActivityLog.actionDtm.goe(startDateTime);
        }
        return adminActivityLog.actionDtm.loe(endDateTime);
    }
}
