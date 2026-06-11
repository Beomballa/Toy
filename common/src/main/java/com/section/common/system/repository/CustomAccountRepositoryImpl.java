package com.section.common.system.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.base.entity.type.YN;
import com.section.common.system.dto.AccountListQuery;
import com.section.common.system.dto.AccountListResDto;
import com.section.common.system.dto.AccountSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.Arrays;
import java.util.List;

import static com.section.common.system.entity.QAccount.account;

public class CustomAccountRepositoryImpl implements CustomAccountRepository {

    private final JPAQueryFactory queryFactory;

    public CustomAccountRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<AccountListResDto> getAccountList(AccountListQuery query, Pageable pageable) {
        BooleanBuilder conditions = buildConditions(query);
        List<AccountListResDto> items = queryFactory
                .select(Projections.bean(
                        AccountListResDto.class,
                        account.id,
                        account.email,
                        account.name,
                        account.nickname,
                        account.masterYn,
                        account.initYn,
                        account.delYn,
                        account.crtDtm
                ))
                .from(account)
                .where(conditions)
                .orderBy(account.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(account.count())
                .from(account)
                .where(conditions);

        return PageableExecutionUtils.getPage(items, pageable, countQuery::fetchOne);
    }

    @Override
    public AccountSummaryDto getAccountSummary(AccountListQuery query) {
        BooleanBuilder conditions = buildConditions(query);
        NumberExpression<Long> totalCount = account.count();
        NumberExpression<Long> masterCount = sumCase(
                new CaseBuilder().when(account.masterYn.eq(YN.Y)).then(1L).otherwise(0L)
        );
        NumberExpression<Long> normalCount = sumCase(
                new CaseBuilder().when(account.masterYn.eq(YN.N)).then(1L).otherwise(0L)
        );
        NumberExpression<Long> deletedCount = sumCase(
                new CaseBuilder().when(account.delYn.eq(YN.Y)).then(1L).otherwise(0L)
        );
        NumberExpression<Long> tempPasswordCount = sumCase(
                new CaseBuilder().when(account.initYn.eq(YN.Y)).then(1L).otherwise(0L)
        );

        Tuple tuple = queryFactory
                .select(totalCount, masterCount, normalCount, deletedCount, tempPasswordCount)
                .from(account)
                .where(conditions)
                .fetchOne();

        if (tuple == null) {
            return new AccountSummaryDto(0, 0, 0, 0, 0);
        }

        return new AccountSummaryDto(
                safeLong(tuple.get(totalCount)),
                safeLong(tuple.get(masterCount)),
                safeLong(tuple.get(normalCount)),
                safeLong(tuple.get(deletedCount)),
                safeLong(tuple.get(tempPasswordCount))
        );
    }

    private BooleanBuilder buildConditions(AccountListQuery query) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(keywordLike(query.keyword()));
        builder.and(masterYnEq(query.masterYn()));
        builder.and(delYnEq(query.delYn()));
        builder.and(initYnEq(query.initYn()));
        return builder;
    }

    private Predicate keywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String[] tokens = Arrays.stream(keyword.trim().split("\\s+"))
                .filter(token -> !token.isBlank())
                .toArray(String[]::new);

        BooleanBuilder builder = new BooleanBuilder();
        for (String token : tokens) {
            builder.and(
                    account.email.containsIgnoreCase(token)
                            .or(account.name.containsIgnoreCase(token))
                            .or(account.nickname.containsIgnoreCase(token))
            );
        }
        return builder.getValue();
    }

    private BooleanExpression masterYnEq(YN masterYn) {
        return masterYn == null ? null : account.masterYn.eq(masterYn);
    }

    private BooleanExpression delYnEq(YN delYn) {
        return delYn == null ? null : account.delYn.eq(delYn);
    }

    private BooleanExpression initYnEq(YN initYn) {
        return initYn == null ? null : account.initYn.eq(initYn);
    }

    private NumberExpression<Long> sumCase(NumberExpression<Long> caseExpression) {
        return Expressions.numberTemplate(Long.class, "sum({0})", caseExpression);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
