package com.section.common.system.repository;

import com.querydsl.core.types.Projections;
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
                .where(historyConditions(query))
                .orderBy(adminSystemSettingHistory.historyNo.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory.select(adminSystemSettingHistory.count())
                .from(adminSystemSettingHistory)
                .where(historyConditions(query));

        return PageableExecutionUtils.getPage(items, pageable, countQuery::fetchOne);
    }

    @Override
    public AdminSystemSettingHistorySummaryDto getHistorySummary(AdminSystemSettingHistoryListQuery query) {
        return new AdminSystemSettingHistorySummaryDto(
                countBy(query, null, null),
                countBy(query, null, LocalDate.now()),
                countBy(query, "SYSTEM_MAINTENANCE_MODE", null),
                countBy(query, "COMMUNITY_WRITE_ENABLED", null),
                countBy(query, "ORDER_EXPORT_ENABLED", null),
                countBy(query, "LOW_STOCK_DEFAULT_THRESHOLD", null)
        );
    }

    private BooleanExpression[] historyConditions(AdminSystemSettingHistoryListQuery query) {
        return new BooleanExpression[]{
                settingKeyEq(query.settingKey()),
                adminNoEq(query.adminNo()),
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

    private long countBy(AdminSystemSettingHistoryListQuery query, String forcedSettingKey, LocalDate forcedStartDate) {
        AdminSystemSettingHistoryListQuery effectiveQuery = new AdminSystemSettingHistoryListQuery(
                forcedSettingKey != null ? forcedSettingKey : query.settingKey(),
                query.adminNo(),
                forcedStartDate != null ? forcedStartDate : query.startDate(),
                forcedStartDate != null ? forcedStartDate : query.endDate()
        );
        Long count = queryFactory
                .select(adminSystemSettingHistory.count())
                .from(adminSystemSettingHistory)
                .where(historyConditions(effectiveQuery))
                .fetchOne();
        return count == null ? 0L : count;
    }
}
