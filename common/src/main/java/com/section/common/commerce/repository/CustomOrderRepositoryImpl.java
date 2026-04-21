package com.section.common.commerce.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.commerce.dto.OrderListReqDto;
import com.section.common.commerce.dto.OrderListResDto;
import com.section.common.commerce.dto.OrderItemResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.section.common.commerce.entity.QOrders.orders;
import static com.section.common.commerce.entity.QOrderItem.orderItem;
import static com.section.common.commerce.entity.QProduct.product;

@Repository
public class CustomOrderRepositoryImpl implements CustomOrderRepository {
    
    private final JPAQueryFactory jpaQueryFactory;

    public CustomOrderRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

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
                        statusEq(reqDto.getStatus()),
                        crtDtmBetween(reqDto.getStartDate(), reqDto.getEndDate())
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
                        statusEq(reqDto.getStatus()),
                        crtDtmBetween(reqDto.getStartDate(), reqDto.getEndDate())
                );

        return PageableExecutionUtils.getPage(list, pageable, countQuery::fetchOne);
    }

    @Override
    public OrderListResDto getOrderDetail(Long orderNo) {
        return jpaQueryFactory
                .select(Projections.bean(OrderListResDto.class,
                        orders.id.as("orderNo"),
                        orders.orderNum.as("orderNum"),
                        orders.buyerName.as("buyerName"),
                        orders.buyerPhone.as("buyerPhone"),
                        orders.totalAmount.as("totalAmount"),
                        orders.status.as("status"),
                        orders.crtDtm.as("crtDtm")
                ))
                .from(orders)
                .where(orders.id.eq(orderNo))
                .fetchOne();
    }

    @Override
    public List<OrderItemResDto> getOrderItems(Long orderNo) {
        return jpaQueryFactory
                .select(Projections.bean(OrderItemResDto.class,
                        orderItem.id.as("orderItemNo"),
                        orderItem.productNo.as("productNo"),
                        orderItem.productName.as("productName"),
                        orderItem.orderPrice.as("orderPrice"),
                        orderItem.count.as("count"),
                        product.thumbnailUrl.as("thumbnailUrl")
                ))
                .from(orderItem)
                .leftJoin(product).on(product.id.eq(orderItem.productNo))
                .where(orderItem.orderNo.eq(orderNo))
                .fetch();
    }

    @Override
    public Map<String, Object> getTodaySummary() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        Map<String, Object> summary = new HashMap<>();

        // 오늘 주문 건수 (전체)
        Long count = jpaQueryFactory
                .select(orders.count())
                .from(orders)
                .where(orders.crtDtm.between(startOfDay, endOfDay))
                .fetchOne();
        summary.put("todayOrderCount", count != null ? count : 0L);

        // 오늘 매출 합계 (결제완료 이상)
        Long sum = jpaQueryFactory
                .select(orders.totalAmount.sumLong())
                .from(orders)
                .where(
                        orders.crtDtm.between(startOfDay, endOfDay),
                        orders.status.ne("CANCELLED")
                )
                .fetchOne();
        summary.put("todayTotalAmount", sum != null ? sum : 0);

        // 상태별 주문 건수 (전체 기간 기준)
        summary.put("preparingCount", getCountByStatus("PREPARING"));
        summary.put("shippingCount", getCountByStatus("SHIPPED"));
        summary.put("cancelledCount", getCountByStatus("CANCELLED"));

        return summary;
    }

    @Override
    public List<Map<String, Object>> getSalesLast7Days() {
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();

        // Querydsl로 날짜별 합계 구하기 (상태가 CANCELLED가 아닌 주문만)
        return jpaQueryFactory
                .select(orders.crtDtm, orders.totalAmount.sumLong())
                .from(orders)
                .where(orders.crtDtm.goe(sevenDaysAgo), orders.status.ne("CANCELLED"))
                .groupBy(orders.crtDtm)
                .orderBy(orders.crtDtm.asc())
                .fetch()
                .stream()
                .map(tuple -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", tuple.get(orders.crtDtm).toLocalDate().toString());
                    map.put("amount", tuple.get(orders.totalAmount.sumLong()));
                    return map;
                }).toList();
    }

    private Long getCountByStatus(String status) {
        return jpaQueryFactory
                .select(orders.count())
                .from(orders)
                .where(orders.status.eq(status))
                .fetchOne();
    }

    @Override
    public List<OrderListResDto> getRecentOrders(int limit) {
        return jpaQueryFactory
                .select(Projections.bean(OrderListResDto.class,
                        orders.id.as("orderNo"),
                        orders.orderNum.as("orderNum"),
                        orders.buyerName.as("buyerName"),
                        orders.totalAmount.as("totalAmount"),
                        orders.status.as("status"),
                        orders.crtDtm.as("crtDtm")
                ))
                .from(orders)
                .orderBy(orders.id.desc())
                .limit(limit)
                .fetch();
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

    private BooleanExpression crtDtmBetween(String startDate, String endDate) {
        if ((startDate == null || startDate.isBlank()) && (endDate == null || endDate.isBlank())) {
            return null;
        }

        LocalDateTime start = null;
        LocalDateTime end = null;

        try {
            if (startDate != null && !startDate.isBlank()) {
                start = LocalDate.parse(startDate).atStartOfDay();
            }
            if (endDate != null && !endDate.isBlank()) {
                end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            }
        } catch (Exception e) {
            return null;
        }

        if (start != null && end != null) {
            return orders.crtDtm.between(start, end);
        } else if (start != null) {
            return orders.crtDtm.goe(start);
        } else if (end != null) {
            return orders.crtDtm.loe(end);
        }

        return null;
    }
}
