package com.section.common.system.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.base.entity.type.YN;
import com.section.common.system.dto.AccountListQuery;
import com.section.common.system.dto.AccountListResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.section.common.system.entity.QAccount.account;

public class CustomAccountRepositoryImpl implements CustomAccountRepository {

    private final JPAQueryFactory queryFactory;

    public CustomAccountRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<AccountListResDto> getAccountList(AccountListQuery query, Pageable pageable) {
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
                .where(accountConditions(query))
                .orderBy(account.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(account.count())
                .from(account)
                .where(accountConditions(query));

        return PageableExecutionUtils.getPage(items, pageable, countQuery::fetchOne);
    }

    private BooleanExpression[] accountConditions(AccountListQuery query) {
        return new BooleanExpression[]{
                keywordLike(query.keyword()),
                masterYnEq(query.masterYn()),
                delYnEq(query.delYn())
        };
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return account.email.containsIgnoreCase(keyword.trim())
                .or(account.name.containsIgnoreCase(keyword.trim()))
                .or(account.nickname.containsIgnoreCase(keyword.trim()));
    }

    private BooleanExpression masterYnEq(YN masterYn) {
        return masterYn == null ? null : account.masterYn.eq(masterYn);
    }

    private BooleanExpression delYnEq(YN delYn) {
        return delYn == null ? null : account.delYn.eq(delYn);
    }
}
