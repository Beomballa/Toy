package com.section.common.commerce.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.base.entity.type.OrderStatus;
import com.section.common.commerce.dto.OrderListItemDto;
import com.section.common.commerce.dto.OrderListQuery;
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
    public Page<OrderListItemDto> getOrderList(OrderListQuery query, Pageable pageable) {
        List<OrderListItemDto> list = jpaQueryFactory
                .select(
                        Projections.bean(
                                OrderListItemDto.class,
                                orders.id.as("orderNo"),
                                orders.orderNum.as("orderNum"),
                                orders.buyerName.as("buyerName"),
                                orders.buyerPhone.as("buyerPhone"),
                                orders.totalAmount.as("totalAmount"),
                                orders.status.as("status"),
                                orders.crtDtm.as("crtDtm"),
                                orderItem.productName.min().as("firstProductName"),
                                orderItem.id.count().as("itemCount")
                        )
                )
                .from(orders)
                .leftJoin(orderItem).on(orderItem.orderNo.eq(orders.id))
                .where(
                        searchKeywordLike(query.searchKeyword()),
                        statusEq(query.status()),
                        crtDtmBetween(query.startDateTime(), query.endDateTime())
                )
                .groupBy(
                        orders.id,
                        orders.orderNum,
                        orders.buyerName,
                        orders.buyerPhone,
                        orders.totalAmount,
                        orders.status,
                        orders.crtDtm
                )
                .orderBy(orders.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(orders.countDistinct())
                .from(orders)
                .leftJoin(orderItem).on(orderItem.orderNo.eq(orders.id))
                .where(
                        searchKeywordLike(query.searchKeyword()),
                        statusEq(query.status()),
                        crtDtmBetween(query.startDateTime(), query.endDateTime())
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
                        orders.crtDtm.as("crtDtm"),
                        orders.deliveryCompany.as("deliveryCompany"),
                        orders.trackingNum.as("trackingNum"),
                        orders.adminMemo.as("adminMemo")
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

    @Override
    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        return jpaQueryFactory
                .select(orderItem.productName, orderItem.count.sumLong())
                .from(orderItem)
                .groupBy(orderItem.productName)
                .orderBy(orderItem.count.sumLong().desc())
                .limit(limit)
                .fetch()
                .stream()
                .map(tuple -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", tuple.get(orderItem.productName));
                    map.put("count", tuple.get(orderItem.count.sumLong()));
                    return map;
                }).toList();
    }

    @Override
    public List<Map<String, Object>> getTopBrandsBySales(int limit) {
        return jpaQueryFactory
                .select(product.brandNo, orders.totalAmount.sumLong())
                .from(orders)
                .join(orderItem).on(orderItem.orderNo.eq(orders.id))
                .join(product).on(product.id.eq(orderItem.productNo))
                .where(orders.status.ne("CANCELLED"))
                .groupBy(product.brandNo)
                .orderBy(orders.totalAmount.sumLong().desc())
                .limit(limit)
                .fetch()
                .stream()
                .map(tuple -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("brandNo", tuple.get(product.brandNo));
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
                        orders.crtDtm.as("crtDtm"),
                        orders.deliveryCompany.as("deliveryCompany"),
                        orders.trackingNum.as("trackingNum"),
                        orders.adminMemo.as("adminMemo")
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
                .or(orders.buyerPhone.containsIgnoreCase(searchKeyword))
                .or(orderItem.productName.containsIgnoreCase(searchKeyword));
    }

    private BooleanExpression statusEq(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return orders.status.eq(status.name());
    }

    private BooleanExpression crtDtmBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null && endDateTime == null) {
            return null;
        }

        if (startDateTime != null && endDateTime != null) {
            return orders.crtDtm.between(startDateTime, endDateTime);
        } else if (startDateTime != null) {
            return orders.crtDtm.goe(startDateTime);
        } else if (endDateTime != null) {
            return orders.crtDtm.loe(endDateTime);
        }

        return null;
    }
}
