package com.section.common.commerce.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.base.entity.type.ProductOrderType;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.ProductListQuery;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductStatsDto;
import com.section.common.commerce.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.section.common.commerce.entity.QBrand.brand;
import static com.section.common.commerce.entity.QCategory.category;
import static com.section.common.commerce.entity.QProduct.product;
import static com.section.common.commerce.entity.QProductOption.productOption;

public class CustomProductRepositoryImpl implements CustomProductRepository {
    public JPAQueryFactory queryFactory;

    public CustomProductRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<ProductListResDto> getProductList(ProductListQuery query, Pageable pageable) {
        List<ProductListResDto> list = queryFactory
                .select(
                        Projections.bean(
                                ProductListResDto.class,
                                product.id.as("productNo"),
                                product.nameKo.as("productName"),
                                product.thumbnailUrl.as("thumbnailUrl"),
                                product.modelNum.as("productModel"),
                                brand.nameKo.as("brandName"),
                                product.releasePrice.as("releasePrice"),
                                productOption.stockCnt.sumLong().coalesce(0L).as("totalStock"),
                                product.status.as("status"),
                                product.crtDtm.as("crtDtm")
                        )
                )
                .from(product)
                .leftJoin(brand).on(brand.brandNo.eq(product.brandNo))
                .leftJoin(category).on(category.categoryNo.eq(product.categoryNo))
                .leftJoin(productOption).on(productOption.productNo.eq(product.id))
                .groupBy(product.id)
                .where(
                        searchKeywordLike(query.searchKeyword()),
                        categoryNoEq(query.categoryNo()),
                        brandNoEq(query.brandNo()),
                        statusEq(query.status()),
                        lowStockOnlyEq(query.lowStockOnly()),
                        notDeleted()
                )
                .orderBy(orderTypeEq(query.orderType()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(product.countDistinct())
                .from(product)
                .leftJoin(brand).on(brand.brandNo.eq(product.brandNo))
                .leftJoin(category).on(category.categoryNo.eq(product.categoryNo))
                .leftJoin(productOption).on(productOption.productNo.eq(product.id))
                .where(
                        searchKeywordLike(query.searchKeyword()),
                        categoryNoEq(query.categoryNo()),
                        brandNoEq(query.brandNo()),
                        statusEq(query.status()),
                        lowStockOnlyEq(query.lowStockOnly()),
                        notDeleted()
                );

        return PageableExecutionUtils.getPage(list, pageable, countQuery::fetchOne);
    }

    @Override
    public ProductStatsDto getProductStats(ProductListQuery query) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        ProductStatsDto stats = new ProductStatsDto();

        // 전체 개수
        Long totalCount = queryFactory
                .select(product.countDistinct())
                .from(product)
                .leftJoin(brand).on(brand.brandNo.eq(product.brandNo))
                .leftJoin(category).on(category.categoryNo.eq(product.categoryNo))
                .where(
                        searchKeywordLike(query.searchKeyword()),
                        categoryNoEq(query.categoryNo()),
                        brandNoEq(query.brandNo()),
                        statusEq(query.status()),
                        lowStockOnlyEq(query.lowStockOnly()),
                        notDeleted()
                )
                .fetchOne();
        stats.setTotalCount(totalCount != null ? totalCount : 0L);

        // 활성 상품 개수
        Long activeCount = queryFactory
                .select(product.countDistinct())
                .from(product)
                .where(
                        searchKeywordLike(query.searchKeyword()),
                        categoryNoEq(query.categoryNo()),
                        brandNoEq(query.brandNo()),
                        statusEq(query.status()),
                        lowStockOnlyEq(query.lowStockOnly()),
                        notDeleted(),
                        product.status.eq(ProductStatus.ACTIVE.name())
                )
                .fetchOne();
        stats.setActiveCount(activeCount != null ? activeCount : 0L);

        // 재고 부족 상품 개수 (예: 100개 미만)
        Long lowStockCount = queryFactory
                .select(product.countDistinct())
                .from(product)
                .where(
                        searchKeywordLike(query.searchKeyword()),
                        categoryNoEq(query.categoryNo()),
                        brandNoEq(query.brandNo()),
                        statusEq(query.status()),
                        lowStockOnlyEq(query.lowStockOnly()),
                        notDeleted(),
                        lowStockProductEq(100L)
                )
                .fetchOne();
        stats.setLowStockCount(lowStockCount != null ? lowStockCount : 0L);

        // 오늘 등록된 상품 개수
        Long todayCount = queryFactory
                .select(product.countDistinct())
                .from(product)
                .where(
                        searchKeywordLike(query.searchKeyword()),
                        categoryNoEq(query.categoryNo()),
                        brandNoEq(query.brandNo()),
                        statusEq(query.status()),
                        lowStockOnlyEq(query.lowStockOnly()),
                        notDeleted(),
                        product.crtDtm.goe(today)
                )
                .fetchOne();
        stats.setTodayCount(todayCount != null ? todayCount : 0L);

        return stats;
    }

    @Override
    public List<ProductListResDto> getLowStockProducts(int threshold, int limit) {
        return queryFactory
                .select(Projections.bean(ProductListResDto.class,
                        product.id.as("productNo"),
                        product.nameKo.as("productName"),
                        brand.nameKo.as("brandName"),
                        productOption.stockCnt.sumLong().as("totalStock")
                ))
                .from(product)
                .leftJoin(brand).on(brand.brandNo.eq(product.brandNo))
                .leftJoin(productOption).on(productOption.productNo.eq(product.id))
                .where(product.status.ne(ProductStatus.DELETE.name()))
                .groupBy(product.id, product.nameKo, brand.nameKo)
                .having(productOption.stockCnt.sumLong().lt((long) threshold))
                .orderBy(productOption.stockCnt.sumLong().asc())
                .limit(limit)
                .fetch();
    }

    public BooleanExpression searchKeywordLike(String searchKeyword) {
        if (searchKeyword == null || searchKeyword.isBlank()) {
            return null;
        }
        return product.nameKo.containsIgnoreCase(searchKeyword)
                .or(product.modelNum.containsIgnoreCase(searchKeyword));
    }

    public BooleanExpression categoryNoEq(Long categoryNo) {
        if (categoryNo == null || categoryNo == 0) {
            return null;
        }
        return product.categoryNo.eq(categoryNo);
    }

    public BooleanExpression brandNoEq(Long brandNo) {
        if (brandNo == null || brandNo == 0) {
            return null;
        }
        return product.brandNo.eq(brandNo);
    }

    public BooleanExpression statusEq(ProductStatus status) {
        if (status == null) {
            return null;
        }
        return product.status.eq(status.name());
    }

    public BooleanExpression notDeleted() {
        return product.status.ne(ProductStatus.DELETE.name());
    }

    // 저재고 집계는 상품별 옵션 합계를 서브쿼리에서 먼저 좁혀서 메모리 count 경고를 피합니다.
    public BooleanExpression lowStockProductEq(Long threshold) {
        if (threshold == null) {
            return null;
        }

        return product.id.in(
                JPAExpressions.select(productOption.productNo)
                        .from(productOption)
                        .groupBy(productOption.productNo)
                        .having(productOption.stockCnt.sumLong().lt(threshold))
        );
    }

    public BooleanExpression lowStockOnlyEq(boolean lowStockOnly) {
        if (!lowStockOnly) {
            return null;
        }
        return lowStockProductEq(100L);
    }

    public OrderSpecifier<?> orderTypeEq(ProductOrderType orderType) {
        if (orderType == null) {
            return product.crtDtm.desc();
        }
        switch (orderType) {
            case RECENT:
                return product.crtDtm.desc();
            case RELEASE_PRICE:
                return product.releasePrice.desc();
            case STOCK_COUNT:
                return productOption.stockCnt.sumLong().desc();
            default:
                return product.crtDtm.desc();
        }
    }
}
