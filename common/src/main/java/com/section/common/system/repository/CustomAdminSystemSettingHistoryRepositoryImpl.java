package com.section.common.system.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.system.dto.AdminSystemSettingHistoryListQuery;
import com.section.common.system.dto.AdminSystemSettingHistoryListResDto;
import com.section.common.system.dto.AdminSystemSettingHistorySummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.section.common.system.entity.QAdminUser.adminUser;
import static com.section.common.system.entity.QAdminSystemSettingHistory.adminSystemSettingHistory;

public class CustomAdminSystemSettingHistoryRepositoryImpl implements CustomAdminSystemSettingHistoryRepository {

    private final JPAQueryFactory queryFactory;

    public CustomAdminSystemSettingHistoryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<AdminSystemSettingHistoryListResDto> getHistoryList(
            AdminSystemSettingHistoryListQuery query,
            Pageable pageable
    ) {
        List<AdminSystemSettingHistoryListResDto> items = queryFactory
                .select(Projections.bean(
                        AdminSystemSettingHistoryListResDto.class,
                        adminSystemSettingHistory.historyNo,
                        adminSystemSettingHistory.settingKey,
                        adminSystemSettingHistory.settingName,
                        adminSystemSettingHistory.beforeValue,
                        adminSystemSettingHistory.afterValue,
                        adminSystemSettingHistory.changeSummary,
                        adminSystemSettingHistory.changedIpAddress,
                        adminSystemSettingHistory.crtNo,
                        adminSystemSettingHistory.crtDtm
                ))
                .from(adminSystemSettingHistory)
                .leftJoin(adminUser).on(adminSystemSettingHistory.crtNo.eq(adminUser.adminNo))
                .where(historyConditions(query))
                .orderBy(adminSystemSettingHistory.historyNo.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory.select(adminSystemSettingHistory.count())
                .from(adminSystemSettingHistory)
                .leftJoin(adminUser).on(adminSystemSettingHistory.crtNo.eq(adminUser.adminNo))
                .where(historyConditions(query));

        return PageableExecutionUtils.getPage(items, pageable, countQuery::fetchOne);
    }

    @Override
    public AdminSystemSettingHistorySummaryDto getHistorySummary(AdminSystemSettingHistoryListQuery query) {
        AdminSystemSettingHistorySummaryDto summary = queryFactory
                .select(Projections.constructor(
                        AdminSystemSettingHistorySummaryDto.class,
                        adminSystemSettingHistory.count(),
                        sumCase(adminSystemSettingHistory.crtDtm.goe(LocalDate.now().atStartOfDay())),
                        sumCase(adminSystemSettingHistory.settingKey.eq("SYSTEM_MAINTENANCE_MODE")),
                        sumCase(adminSystemSettingHistory.settingKey.eq("COMMUNITY_WRITE_ENABLED")),
                        sumCase(adminSystemSettingHistory.settingKey.eq("ORDER_EXPORT_ENABLED")),
                        sumCase(adminSystemSettingHistory.settingKey.eq("LOW_STOCK_DEFAULT_THRESHOLD"))
                ))
                .from(adminSystemSettingHistory)
                .leftJoin(adminUser).on(adminSystemSettingHistory.crtNo.eq(adminUser.adminNo))
                .where(historyConditions(query))
                .fetchOne();
        return summary == null ? new AdminSystemSettingHistorySummaryDto(0, 0, 0, 0, 0, 0) : summary;
    }

    private BooleanExpression[] historyConditions(AdminSystemSettingHistoryListQuery query) {
        return new BooleanExpression[]{
                settingKeyEq(query.settingKey()),
                adminNoEq(query.adminNo()),
                adminKeywordLike(query.adminKeyword()),
                createdDateBetween(query.startDate(), query.endDate())
        };
    }

    private BooleanExpression settingKeyEq(String settingKey) {
        if (settingKey == null || settingKey.isBlank()) {
            return null;
        }
        return adminSystemSettingHistory.settingKey.eq(settingKey.trim());
    }

    private BooleanExpression adminNoEq(Long adminNo) {
        return adminNo == null ? null : adminSystemSettingHistory.crtNo.eq(adminNo);
    }

    private BooleanExpression adminKeywordLike(String adminKeyword) {
        if (adminKeyword == null || adminKeyword.isBlank()) {
            return null;
        }
        return adminUser.name.containsIgnoreCase(adminKeyword)
                .or(adminUser.loginId.containsIgnoreCase(adminKeyword));
    }

    private BooleanExpression createdDateBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        LocalDateTime startDateTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        if (startDateTime != null && endDateTime != null) {
            return adminSystemSettingHistory.crtDtm.between(startDateTime, endDateTime);
        }
        if (startDateTime != null) {
            return adminSystemSettingHistory.crtDtm.goe(startDateTime);
        }
        return adminSystemSettingHistory.crtDtm.loe(endDateTime);
    }

    private com.querydsl.core.types.Expression<Long> sumCase(BooleanExpression condition) {
        return Expressions.numberTemplate(
                Long.class,
                "sum(case when {0} then 1 else 0 end)",
                condition
        );
    }
}
