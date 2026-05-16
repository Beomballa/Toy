package com.section.common.commerce.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.base.entity.type.ProductHistoryActionType;
import com.section.common.base.entity.type.ProductHistoryOrderType;
import com.section.common.commerce.dto.ProductHistoryListQuery;
import com.section.common.commerce.dto.ProductHistoryListResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.section.common.commerce.entity.QProductChangeHistory.productChangeHistory;
import static com.section.common.system.entity.QAdminUser.adminUser;

public class CustomProductChangeHistoryRepositoryImpl implements CustomProductChangeHistoryRepository {

    private final JPAQueryFactory queryFactory;

    public CustomProductChangeHistoryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<ProductHistoryListResDto> getProductHistoryList(ProductHistoryListQuery query, Pageable pageable) {
        List<ProductHistoryListResDto> items = queryFactory
                .select(Projections.bean(
                        ProductHistoryListResDto.class,
                        productChangeHistory.historyNo,
                        productChangeHistory.productNo,
                        productChangeHistory.actionType.stringValue().as("actionType"),
                        productChangeHistory.summary,
                        productChangeHistory.statusSnapshot,
                        productChangeHistory.optionCount,
                        productChangeHistory.totalStock,
                        productChangeHistory.crtNo.as("actorNo"),
                        adminUser.name.as("actorName"),
                        productChangeHistory.crtDtm.as("actionDtm")
                ))
                .from(productChangeHistory)
                // 이력 엔티티에 직접 연관관계가 없어서, 작업자명 조회는 관리자 번호 기준 조인으로 묶는다.
                .leftJoin(adminUser).on(productChangeHistory.crtNo.eq(adminUser.adminNo))
                .where(historyConditions(query))
                .orderBy(resolveOrderType(query.orderType()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(productChangeHistory.count())
                .from(productChangeHistory)
                .leftJoin(adminUser).on(productChangeHistory.crtNo.eq(adminUser.adminNo))
                .where(historyConditions(query));

        return PageableExecutionUtils.getPage(items, pageable, countQuery::fetchOne);
    }

    private BooleanExpression[] historyConditions(ProductHistoryListQuery query) {
        return new BooleanExpression[]{
                productNoEq(query.productNo()),
                actionTypeEq(query.actionType()),
                keywordLike(query.keyword()),
                actorKeywordLike(query.actorKeyword()),
                actionDateBetween(query.startDate(), query.endDate())
        };
    }

    private BooleanExpression productNoEq(Long productNo) {
        return productNo == null ? null : productChangeHistory.productNo.eq(productNo);
    }

    private BooleanExpression actionTypeEq(ProductHistoryActionType actionType) {
        return actionType == null ? null : productChangeHistory.actionType.eq(actionType);
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return productChangeHistory.summary.containsIgnoreCase(keyword.trim());
    }

    private BooleanExpression actorKeywordLike(String actorKeyword) {
        if (actorKeyword == null || actorKeyword.isBlank()) {
            return null;
        }
        return adminUser.name.containsIgnoreCase(actorKeyword.trim());
    }

    private BooleanExpression actionDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }

        LocalDateTime startDateTime = startDate == null ? null : startDate.atStartOfDay();
        // 종료일은 해당 날짜 23:59:59.999999999까지 포함해야 운영 화면의 일자 검색 기대와 맞습니다.
        LocalDateTime endDateTime = endDate == null ? null : endDate.atTime(LocalTime.MAX);

        if (startDateTime != null && endDateTime != null) {
            return productChangeHistory.crtDtm.between(startDateTime, endDateTime);
        }
        if (startDateTime != null) {
            return productChangeHistory.crtDtm.goe(startDateTime);
        }
        return productChangeHistory.crtDtm.loe(endDateTime);
    }

    private com.querydsl.core.types.OrderSpecifier<?> resolveOrderType(ProductHistoryOrderType orderType) {
        if (orderType == ProductHistoryOrderType.OLDEST) {
            return productChangeHistory.historyNo.asc();
        }
        return productChangeHistory.historyNo.desc();
    }
}
