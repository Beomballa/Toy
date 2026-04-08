package com.section.common.commerce.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.commerce.dto.OrderListReqDto;
import com.section.common.commerce.dto.OrderListResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.section.common.commerce.entity.QOrders.orders;

@Repository
@RequiredArgsConstructor
public class CustomOrderRepositoryImpl implements CustomOrderRepository {
    
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<OrderListResDto> getOrderList(OrderListReqDto reqDto, Pageable pageable) {
        List<OrderListResDto> list = jpaQueryFactory
                .select(
                        Projections.bean(
                                OrderListResDto.class,
                                orders.id.as("orderNo"),
                                orders.orderNum.as("orderNum"),
                                orders.buyerName.as("buyerName"),
                                orders.buyerPhone.as("buyerPhone"),
                                orders.totalAmount.as("totalAmount"),
                                orders.status.as("status"),
                                orders.crtDtm.as("crtDtm")
                        )
                )
                .from(orders)
                .where(
                        searchKeywordLike(reqDto.getSearchKeyword()),
                        statusEq(reqDto.getStatus())
                )
                .orderBy(orders.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(orders.count())
                .from(orders)
                .where(
                        searchKeywordLike(reqDto.getSearchKeyword()),
                        statusEq(reqDto.getStatus())
                );

        return PageableExecutionUtils.getPage(list, pageable, countQuery::fetchOne);
    }

    private BooleanExpression searchKeywordLike(String searchKeyword) {
        if (searchKeyword == null || searchKeyword.isBlank()) {
            return null;
        }
        return orders.orderNum.containsIgnoreCase(searchKeyword)
                .or(orders.buyerName.containsIgnoreCase(searchKeyword))
                .or(orders.buyerPhone.containsIgnoreCase(searchKeyword));
    }

    private BooleanExpression statusEq(String statusVal) {
        if (statusVal == null || statusVal.isBlank()) {
            return null;
        }
        return orders.status.stringValue().eq(statusVal);
    }
}
