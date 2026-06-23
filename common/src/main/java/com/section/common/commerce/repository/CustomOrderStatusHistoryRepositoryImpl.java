package com.section.common.commerce.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.base.entity.type.OrderHistoryOrderType;
import com.section.common.commerce.dto.OrderHistoryListQuery;
import com.section.common.commerce.dto.OrderHistoryListResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static com.section.common.commerce.entity.QOrderStatusHistory.orderStatusHistory;
import static com.section.common.system.entity.QAdminUser.adminUser;

public class CustomOrderStatusHistoryRepositoryImpl implements CustomOrderStatusHistoryRepository {

    private final JPAQueryFactory queryFactory;

    public CustomOrderStatusHistoryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<OrderHistoryListResDto> getOrderHistoryList(OrderHistoryListQuery query, Pageable pageable) {
        List<OrderHistoryListResDto> items = queryFactory
                .select(Projections.bean(
                        OrderHistoryListResDto.class,
                        orderStatusHistory.id.as("historyNo"),
                        orderStatusHistory.orderNo,
                        orderStatusHistory.actionType,
                        orderStatusHistory.beforeStatus,
                        orderStatusHistory.afterStatus,
                        orderStatusHistory.reason,
                        orderStatusHistory.adminMemoSnapshot,
                        orderStatusHistory.deliveryCompany,
                        orderStatusHistory.trackingNum,
                        orderStatusHistory.crtNo.as("actorNo"),
                        adminUser.name.as("actorName"),
                        orderStatusHistory.crtDtm.as("actionDtm")
                ))
                .from(orderStatusHistory)
                // 주문 이력도 직접 연관관계가 없어 감사 번호 기준으로 관리자명을 조인합니다.
                .leftJoin(adminUser).on(orderStatusHistory.crtNo.eq(adminUser.adminNo))
                .where(historyConditions(query))
                .orderBy(resolveOrderType(query.orderType()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(orderStatusHistory.count())
                .from(orderStatusHistory)
                .leftJoin(adminUser).on(orderStatusHistory.crtNo.eq(adminUser.adminNo))
                .where(historyConditions(query));

        return PageableExecutionUtils.getPage(items, pageable, countQuery::fetchOne);
    }

    private BooleanExpression[] historyConditions(OrderHistoryListQuery query) {
        return new BooleanExpression[]{
                orderNoEq(query.orderNo()),
                actionTypeEq(query.actionType()),
                keywordLike(query.keyword()),
                actorNoEq(query.actorNo()),
                actorKeywordLike(query.actorKeyword()),
                actionDateBetween(query.startDate(), query.endDate())
        };
    }

    private BooleanExpression orderNoEq(Long orderNo) {
        return orderNo == null ? null : orderStatusHistory.orderNo.eq(orderNo);
    }

    private BooleanExpression actionTypeEq(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return null;
        }
        return orderStatusHistory.actionType.eq(actionType);
    }

    private BooleanExpression actorNoEq(Long actorNo) {
        return actorNo == null ? null : orderStatusHistory.crtNo.eq(actorNo);
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        List<String> terms = Arrays.stream(keyword.trim().split("\\s+"))
                .filter(term -> !term.isBlank())
                .toList();

        BooleanExpression predicate = null;
        for (String term : terms) {
            BooleanExpression termPredicate = orderStatusHistory.reason.containsIgnoreCase(term)
                    .or(orderStatusHistory.adminMemoSnapshot.containsIgnoreCase(term))
                    .or(orderStatusHistory.deliveryCompany.containsIgnoreCase(term))
                    .or(orderStatusHistory.trackingNum.containsIgnoreCase(term));

            String digitTerm = term.replaceAll("[^0-9]", "");
            if (!digitTerm.isBlank()) {
                termPredicate = termPredicate.or(normalizedTrackingNum().contains(digitTerm));
            }

            predicate = predicate == null ? termPredicate : predicate.and(termPredicate);
        }

        return predicate;
    }

    private BooleanExpression actorKeywordLike(String actorKeyword) {
        if (actorKeyword == null || actorKeyword.isBlank()) {
            return null;
        }

        List<String> terms = Arrays.stream(actorKeyword.trim().split("\\s+"))
                .filter(term -> !term.isBlank())
                .toList();

        BooleanExpression predicate = null;
        for (String term : terms) {
            BooleanExpression termPredicate = adminUser.name.containsIgnoreCase(term);
            predicate = predicate == null ? termPredicate : predicate.and(termPredicate);
        }
        return predicate;
    }

    private BooleanExpression actionDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }

        LocalDateTime startDateTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate == null ? null : endDate.atTime(LocalTime.MAX);

        if (startDateTime != null && endDateTime != null) {
            return orderStatusHistory.crtDtm.between(startDateTime, endDateTime);
        }
        if (startDateTime != null) {
            return orderStatusHistory.crtDtm.goe(startDateTime);
        }
        return orderStatusHistory.crtDtm.loe(endDateTime);
    }

    private OrderSpecifier<?> resolveOrderType(OrderHistoryOrderType orderType) {
        if (orderType == OrderHistoryOrderType.OLDEST) {
            return orderStatusHistory.id.asc();
        }
        return orderStatusHistory.id.desc();
    }

    private StringExpression normalizedTrackingNum() {
        return com.querydsl.core.types.dsl.Expressions.stringTemplate(
                "replace(replace(replace({0}, '-', ''), ' ', ''), '_', '')",
                orderStatusHistory.trackingNum
        );
    }
}
