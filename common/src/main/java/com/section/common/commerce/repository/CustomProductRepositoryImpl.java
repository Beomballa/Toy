package com.section.common.commerce.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.base.entity.type.ProductOrderType;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.AdminFrontDisplayProductQuery;
import com.section.common.commerce.dto.AdminFrontDisplayProductRow;
import com.section.common.commerce.dto.FrontCatalogProductRow;
import com.section.common.commerce.dto.FrontCatalogQuery;
import com.section.common.commerce.dto.ProductListQuery;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductStatsDto;
import com.section.common.commerce.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.section.common.commerce.entity.QBrand.brand;
import static com.section.common.commerce.entity.QCategory.category;
import static com.section.common.commerce.entity.QFrontProductDisplay.frontProductDisplay;
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
                .select(productListProjection())
                .from(product)
                .leftJoin(brand).on(brand.brandNo.eq(product.brandNo))
                .leftJoin(category).on(category.categoryNo.eq(product.categoryNo))
                .leftJoin(productOption).on(productOption.productNo.eq(product.id))
                .groupBy(product.id)
                .where(productListConditions(query))
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
                .where(productListConditions(query));

        return PageableExecutionUtils.getPage(list, pageable, countQuery::fetchOne);
    }

    @Override
    public List<ProductListResDto> getProductExportList(ProductListQuery query, int limit) {
        // export는 화면 페이지네이션과 분리해서 같은 필터만 재사용하고, 건수만 별도 정책으로 제한합니다.
        return queryFactory
                .select(productListProjection())
                .from(product)
                .leftJoin(brand).on(brand.brandNo.eq(product.brandNo))
                .leftJoin(category).on(category.categoryNo.eq(product.categoryNo))
                .leftJoin(productOption).on(productOption.productNo.eq(product.id))
                .groupBy(product.id)
                .where(productListConditions(query))
                .orderBy(orderTypeEq(query.orderType()))
                .limit(limit)
                .fetch();
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
                .where(productStatConditions(query))
                .fetchOne();
        stats.setTotalCount(totalCount != null ? totalCount : 0L);

        // 활성 상품 개수
        Long activeCount = queryFactory
                .select(product.countDistinct())
                .from(product)
                .leftJoin(brand).on(brand.brandNo.eq(product.brandNo))
                .leftJoin(category).on(category.categoryNo.eq(product.categoryNo))
                .where(productStatConditions(query, product.status.eq(ProductStatus.ACTIVE.name())))
                .fetchOne();
        stats.setActiveCount(activeCount != null ? activeCount : 0L);

        // 재고 부족 상품 개수 (예: 100개 미만)
        Long lowStockCount = queryFactory
                .select(product.countDistinct())
                .from(product)
                .leftJoin(brand).on(brand.brandNo.eq(product.brandNo))
                .leftJoin(category).on(category.categoryNo.eq(product.categoryNo))
                .where(productStatConditions(query, lowStockProductEq(query.effectiveLowStockThreshold())))
                .fetchOne();
        stats.setLowStockCount(lowStockCount != null ? lowStockCount : 0L);

        // 오늘 등록된 상품 개수
        Long todayCount = queryFactory
                .select(product.countDistinct())
                .from(product)
                .leftJoin(brand).on(brand.brandNo.eq(product.brandNo))
                .leftJoin(category).on(category.categoryNo.eq(product.categoryNo))
                .where(productStatConditions(query, product.crtDtm.goe(today)))
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

    @Override
    public List<Long> getReferencedBrandNos(Collection<Long> brandNos) {
        if (brandNos == null || brandNos.isEmpty()) {
            return List.of();
        }
        return queryFactory
                .select(product.brandNo)
                .from(product)
                .where(product.brandNo.in(brandNos))
                .groupBy(product.brandNo)
                .fetch();
    }

    @Override
    public List<Long> getReferencedCategoryNos(Collection<Long> categoryNos) {
        if (categoryNos == null || categoryNos.isEmpty()) {
            return List.of();
        }
        return queryFactory
                .select(product.categoryNo)
                .from(product)
                .where(product.categoryNo.in(categoryNos))
                .groupBy(product.categoryNo)
                .fetch();
    }

    @Override
    public List<FrontCatalogProductRow> getFrontCatalogProducts(FrontCatalogQuery query) {
        return frontCatalogBaseQuery()
                .where(frontCatalogConditions(query))
                .groupBy(
                        product.id,
                        product.brandNo,
                        product.categoryNo,
                        brand.nameKo,
                        category.name,
                        product.nameKo,
                        frontProductDisplay.headline,
                        product.modelNum,
                        product.releasePrice,
                        product.crtDtm,
                        frontProductDisplay.description,
                        frontProductDisplay.mood,
                        frontProductDisplay.featuredYn,
                        frontProductDisplay.featuredRank
                )
                .having(frontStockCondition(query))
                .orderBy(frontCatalogOrder(query))
                .fetch();
    }

    @Override
    public Optional<FrontCatalogProductRow> getFrontCatalogProduct(Long productNo) {
        return Optional.ofNullable(
                frontCatalogBaseQuery()
                        .where(product.id.eq(productNo), product.status.eq(ProductStatus.ACTIVE.name()))
                        .groupBy(
                                product.id,
                                product.brandNo,
                                product.categoryNo,
                                brand.nameKo,
                                category.name,
                                product.nameKo,
                                frontProductDisplay.headline,
                                product.modelNum,
                                product.releasePrice,
                                product.crtDtm,
                                frontProductDisplay.description,
                                frontProductDisplay.mood,
                                frontProductDisplay.featuredYn,
                                frontProductDisplay.featuredRank
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<FrontCatalogProductRow> getRelatedFrontCatalogProducts(Long productNo, Long brandNo, Long categoryNo, int limit) {
        return frontCatalogBaseQuery()
                .where(
                        product.id.ne(productNo),
                        product.status.eq(ProductStatus.ACTIVE.name()),
                        product.brandNo.eq(brandNo).or(product.categoryNo.eq(categoryNo))
                )
                .groupBy(
                        product.id,
                        product.brandNo,
                        product.categoryNo,
                        brand.nameKo,
                        category.name,
                        product.nameKo,
                        frontProductDisplay.headline,
                        product.modelNum,
                        product.releasePrice,
                        product.crtDtm,
                        frontProductDisplay.description,
                        frontProductDisplay.mood,
                        frontProductDisplay.featuredYn,
                        frontProductDisplay.featuredRank
                )
                .orderBy(frontFeaturedOrder(), totalStockSum().asc(), product.releaseDt.desc(), product.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<AdminFrontDisplayProductRow> getAdminFrontDisplayProducts(AdminFrontDisplayProductQuery query) {
        return queryFactory
                .select(Projections.constructor(
                        AdminFrontDisplayProductRow.class,
                        product.id,
                        product.nameKo,
                        brand.nameKo,
                        category.name,
                        product.releasePrice,
                        totalStockSum(),
                        product.status,
                        frontProductDisplay.displayNo.isNotNull(),
                        new CaseBuilder().when(adminFrontDisplayContentReady()).then(true).otherwise(false),
                        frontProductDisplay.headline,
                        frontProductDisplay.description,
                        frontProductDisplay.mood,
                        new CaseBuilder().when(frontProductDisplay.featuredYn.eq("Y")).then(true).otherwise(false),
                        frontProductDisplay.featuredRank,
                        product.thumbnailUrl
                ))
                .from(product)
                .leftJoin(frontProductDisplay).on(frontProductDisplay.productNo.eq(product.id))
                .leftJoin(brand).on(brand.brandNo.eq(product.brandNo))
                .leftJoin(category).on(category.categoryNo.eq(product.categoryNo))
                .leftJoin(productOption).on(productOption.productNo.eq(product.id))
                .where(
                        product.status.ne(ProductStatus.DELETE.name()),
                        query.featuredOnly() ? frontProductDisplay.featuredYn.eq("Y") : null,
                        query.status() == null ? null : product.status.eq(query.status().name()),
                        brandNoEq(query.brandNo()),
                        categoryNoEq(query.categoryNo()),
                        adminFrontDisplayKeywordLike(query.keyword()),
                        adminFrontDisplayConfiguredEq(query),
                        adminFrontDisplayContentStatusEq(query)
                )
                .groupBy(
                        product.id,
                        product.nameKo,
                        brand.nameKo,
                        category.name,
                        product.releasePrice,
                        product.status,
                        frontProductDisplay.displayNo,
                        frontProductDisplay.headline,
                        frontProductDisplay.description,
                        frontProductDisplay.mood,
                        frontProductDisplay.featuredYn,
                        frontProductDisplay.featuredRank
                )
                .having(adminFrontDisplayHaving(query))
                .orderBy(
                        adminFrontDisplayOrder(query)
                )
                .fetch();
    }

    private BooleanExpression adminFrontDisplayKeywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        List<String> terms = Arrays.stream(keyword.trim().split("\\s+"))
                .filter(term -> !term.isBlank())
                .toList();
        BooleanExpression predicate = null;
        for (String term : terms) {
            BooleanExpression termPredicate = product.nameKo.containsIgnoreCase(term)
                    .or(product.modelNum.containsIgnoreCase(term))
                    .or(brand.nameKo.containsIgnoreCase(term))
                    .or(category.name.containsIgnoreCase(term))
                    .or(frontProductDisplay.headline.containsIgnoreCase(term))
                    .or(frontProductDisplay.description.containsIgnoreCase(term))
                    .or(frontProductDisplay.mood.containsIgnoreCase(term));

            String normalizedModelTerm = term.replaceAll("[^A-Za-z0-9]", "");
            if (!normalizedModelTerm.isBlank()) {
                termPredicate = termPredicate.or(normalizedModelNum().containsIgnoreCase(normalizedModelTerm));
            }
            predicate = predicate == null ? termPredicate : predicate.and(termPredicate);
        }
        return predicate;
    }

    private BooleanExpression adminFrontDisplayConfiguredEq(AdminFrontDisplayProductQuery query) {
        if (query.configuredOnly()) {
            return frontProductDisplay.displayNo.isNotNull();
        }
        if (query.unconfiguredOnly()) {
            return frontProductDisplay.displayNo.isNull();
        }
        return null;
    }

    private BooleanExpression adminFrontDisplayContentStatusEq(AdminFrontDisplayProductQuery query) {
        if (query.readyContentOnly()) {
            return adminFrontDisplayContentReady();
        }
        if (query.incompleteContentOnly()) {
            return adminFrontDisplayContentIncomplete();
        }
        return null;
    }

    private BooleanExpression adminFrontDisplayContentReady() {
        return frontProductDisplay.displayNo.isNotNull()
                .and(hasDisplayText(frontProductDisplay.headline))
                .and(hasDisplayText(frontProductDisplay.description))
                .and(hasDisplayText(frontProductDisplay.mood));
    }

    private BooleanExpression adminFrontDisplayContentIncomplete() {
        return frontProductDisplay.displayNo.isNull()
                .or(hasDisplayText(frontProductDisplay.headline).not())
                .or(hasDisplayText(frontProductDisplay.description).not())
                .or(hasDisplayText(frontProductDisplay.mood).not());
    }

    private BooleanExpression hasDisplayText(StringPath path) {
        return path.isNotNull()
                .and(Expressions.stringTemplate("trim({0})", path).ne(""));
    }

    private BooleanExpression adminFrontDisplayHaving(AdminFrontDisplayProductQuery query) {
        if (!query.lowStockOnly()) {
            return null;
        }
        return totalStockSum().lt(query.lowStockThreshold());
    }

    private OrderSpecifier<?>[] adminFrontDisplayOrder(AdminFrontDisplayProductQuery query) {
        return switch (query.sort()) {
            case "LATEST" -> new OrderSpecifier<?>[]{
                    frontFeaturedOrder(),
                    product.crtDtm.desc(),
                    product.id.desc()
            };
            case "STOCK_ASC" -> new OrderSpecifier<?>[]{
                    frontFeaturedOrder(),
                    totalStockSum().asc(),
                    product.id.desc()
            };
            case "STOCK_DESC" -> new OrderSpecifier<?>[]{
                    frontFeaturedOrder(),
                    totalStockSum().desc(),
                    product.id.desc()
            };
            case "PRICE_HIGH" -> new OrderSpecifier<?>[]{
                    frontFeaturedOrder(),
                    product.releasePrice.desc(),
                    product.id.desc()
            };
            case "PRICE_LOW" -> new OrderSpecifier<?>[]{
                    frontFeaturedOrder(),
                    product.releasePrice.asc(),
                    product.id.desc()
            };
            default -> new OrderSpecifier<?>[]{
                    frontFeaturedOrder(),
                    frontProductDisplay.featuredRank.asc().nullsLast(),
                    totalStockSum().asc(),
                    product.id.desc()
            };
        };
    }

    public BooleanExpression searchKeywordLike(String searchKeyword) {
        if (searchKeyword == null || searchKeyword.isBlank()) {
            return null;
        }

        List<String> terms = Arrays.stream(searchKeyword.trim().split("\\s+"))
                .filter(term -> !term.isBlank())
                .toList();

        BooleanExpression predicate = null;
        for (String term : terms) {
            BooleanExpression termPredicate = product.nameKo.containsIgnoreCase(term)
                    .or(product.modelNum.containsIgnoreCase(term))
                    .or(brand.nameKo.containsIgnoreCase(term))
                    .or(category.name.containsIgnoreCase(term));

            String normalizedModelTerm = term.replaceAll("[^A-Za-z0-9]", "");
            if (!normalizedModelTerm.isBlank()) {
                termPredicate = termPredicate.or(normalizedModelNum().containsIgnoreCase(normalizedModelTerm));
            }

            predicate = predicate == null ? termPredicate : predicate.and(termPredicate);
        }

        return predicate;
    }

    public BooleanExpression categoryNoEq(Long categoryNo) {
        if (categoryNo == null || categoryNo == 0) {
            return null;
        }
        return category.categoryNo.eq(categoryNo)
                .or(category.parentNo.eq(categoryNo));
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

    public BooleanExpression lowStockOnlyEq(boolean lowStockOnly, Long threshold) {
        if (!lowStockOnly) {
            return null;
        }
        return lowStockProductEq(threshold == null ? 100L : threshold);
    }

    private BooleanExpression[] productListConditions(ProductListQuery query) {
        return new BooleanExpression[] {
                searchKeywordLike(query.searchKeyword()),
                categoryNoEq(query.categoryNo()),
                brandNoEq(query.brandNo()),
                statusEq(query.status()),
                lowStockOnlyEq(query.lowStockOnly(), query.lowStockThreshold()),
                createdTodayOnlyEq(query.createdTodayOnly()),
                notDeleted()
        };
    }

    private BooleanExpression[] productStatConditions(ProductListQuery query) {
        // 통계 카드도 목록과 같은 빠른 필터 문맥을 따라가야 숫자와 실제 결과가 어긋나지 않습니다.
        return new BooleanExpression[]{
                searchKeywordLike(query.searchKeyword()),
                categoryNoEq(query.categoryNo()),
                brandNoEq(query.brandNo()),
                statusEq(query.status()),
                lowStockOnlyEq(query.lowStockOnly(), query.lowStockThreshold()),
                createdTodayOnlyEq(query.createdTodayOnly()),
                notDeleted()
        };
    }

    private BooleanExpression[] productStatConditions(ProductListQuery query, BooleanExpression extraCondition) {
        BooleanExpression[] baseConditions = productStatConditions(query);
        BooleanExpression[] mergedConditions = Arrays.copyOf(baseConditions, baseConditions.length + 1);
        mergedConditions[baseConditions.length] = extraCondition;
        return mergedConditions;
    }

    private com.querydsl.core.types.QBean<ProductListResDto> productListProjection() {
        return Projections.bean(
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
        );
    }

    public BooleanExpression createdTodayOnlyEq(boolean createdTodayOnly) {
        if (!createdTodayOnly) {
            return null;
        }

        LocalDateTime startOfToday = LocalDateTime.now().with(LocalTime.MIN);
        return product.crtDtm.goe(startOfToday);
    }

    private StringExpression normalizedModelNum() {
        return com.querydsl.core.types.dsl.Expressions.stringTemplate(
                "replace(replace(replace({0}, '-', ''), ' ', ''), '_', '')",
                product.modelNum
        );
    }

    private JPAQuery<FrontCatalogProductRow> frontCatalogBaseQuery() {
        return queryFactory
                .select(Projections.constructor(
                        FrontCatalogProductRow.class,
                        product.id,
                        product.brandNo,
                        product.categoryNo,
                        brand.nameKo,
                        category.name,
                        product.nameKo,
                        frontProductDisplay.headline,
                        product.modelNum,
                        product.releasePrice,
                        totalStockSum().intValue(),
                        product.crtDtm,
                        frontProductDisplay.description,
                        frontProductDisplay.mood,
                        new CaseBuilder().when(frontProductDisplay.featuredYn.eq("Y")).then(true).otherwise(false),
                        frontProductDisplay.featuredRank,
                        product.thumbnailUrl
                ))
                .from(product)
                .leftJoin(frontProductDisplay).on(frontProductDisplay.productNo.eq(product.id))
                .leftJoin(brand).on(brand.brandNo.eq(product.brandNo))
                .leftJoin(category).on(category.categoryNo.eq(product.categoryNo))
                .leftJoin(productOption).on(productOption.productNo.eq(product.id));
    }

    private BooleanExpression[] frontCatalogConditions(FrontCatalogQuery query) {
        return new BooleanExpression[]{
                product.status.eq(ProductStatus.ACTIVE.name()),
                frontKeywordLike(query.keyword()),
                frontBrandEq(query.brand()),
                frontCategoryEq(query.category()),
                frontFeaturedOnlyEq(query.featuredOnly()),
                frontPriceBandEq(query)
        };
    }

    private BooleanExpression frontKeywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        List<String> terms = Arrays.stream(keyword.trim().split("\\s+"))
                .filter(term -> !term.isBlank())
                .toList();
        BooleanExpression predicate = null;
        for (String term : terms) {
            BooleanExpression termPredicate = product.nameKo.containsIgnoreCase(term)
                    .or(product.modelNum.containsIgnoreCase(term))
                    .or(brand.nameKo.containsIgnoreCase(term))
                    .or(category.name.containsIgnoreCase(term))
                    .or(frontProductDisplay.headline.containsIgnoreCase(term))
                    .or(frontProductDisplay.description.containsIgnoreCase(term))
                    .or(frontProductDisplay.mood.containsIgnoreCase(term));
            String normalizedModelTerm = term.replaceAll("[^A-Za-z0-9]", "");
            if (!normalizedModelTerm.isBlank()) {
                termPredicate = termPredicate.or(normalizedModelNum().containsIgnoreCase(normalizedModelTerm));
            }
            predicate = predicate == null ? termPredicate : predicate.and(termPredicate);
        }
        return predicate;
    }

    private BooleanExpression frontBrandEq(String brandName) {
        if (brandName == null || brandName.isBlank()) {
            return null;
        }
        return brand.nameKo.eq(brandName);
    }

    private BooleanExpression frontCategoryEq(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        return category.name.eq(categoryName);
    }

    private BooleanExpression frontFeaturedOnlyEq(boolean featuredOnly) {
        if (!featuredOnly) {
            return null;
        }
        return frontProductDisplay.featuredYn.eq("Y");
    }

    private BooleanExpression frontPriceBandEq(FrontCatalogQuery query) {
        if (query.isUnder200Only()) {
            return product.releasePrice.lt(200000);
        }
        if (query.isBetween200And300Only()) {
            return product.releasePrice.between(200000, 300000);
        }
        if (query.isOver300Only()) {
            return product.releasePrice.gt(300000);
        }
        return null;
    }

    private BooleanExpression frontStockCondition(FrontCatalogQuery query) {
        if (query.isLowStockOnly()) {
            return totalStockSum().lt((long) query.lowStockThreshold());
        }
        if (query.isStableStockOnly()) {
            return totalStockSum().goe((long) query.lowStockThreshold());
        }
        return null;
    }

    private OrderSpecifier<?>[] frontCatalogOrder(FrontCatalogQuery query) {
        return switch (query.sort()) {
            case "PRICE_HIGH" -> new OrderSpecifier<?>[]{frontFeaturedOrder(), product.releasePrice.desc(), product.id.desc()};
            case "PRICE_LOW" -> new OrderSpecifier<?>[]{frontFeaturedOrder(), product.releasePrice.asc(), product.id.desc()};
            case "NAME_ASC" -> new OrderSpecifier<?>[]{frontFeaturedOrder(), product.nameKo.asc(), product.id.desc()};
            case "STOCK_ASC" -> new OrderSpecifier<?>[]{frontFeaturedOrder(), totalStockSum().asc(), product.id.desc()};
            case "STOCK_DESC" -> new OrderSpecifier<?>[]{frontFeaturedOrder(), totalStockSum().desc(), product.id.desc()};
            case "FEATURED" -> new OrderSpecifier<?>[]{frontFeaturedOrder(), frontProductDisplay.featuredRank.asc().nullsLast(), product.id.desc()};
            default -> new OrderSpecifier<?>[]{frontFeaturedOrder(), product.crtDtm.desc(), product.id.desc()};
        };
    }

    private OrderSpecifier<Integer> frontFeaturedOrder() {
        return new CaseBuilder()
                .when(frontProductDisplay.featuredYn.eq("Y")).then(0)
                .otherwise(1)
                .asc();
    }

    private NumberExpression<Long> totalStockSum() {
        return productOption.stockCnt.sumLong().coalesce(0L);
    }

    public OrderSpecifier<?>[] orderTypeEq(ProductOrderType orderType) {
        if (orderType == null) {
            return new OrderSpecifier<?>[]{product.crtDtm.desc(), product.id.desc()};
        }
        return switch (orderType) {
            case RECENT -> new OrderSpecifier<?>[]{product.crtDtm.desc(), product.id.desc()};
            case RELEASE_PRICE -> new OrderSpecifier<?>[]{product.releasePrice.desc(), product.id.desc()};
            case STOCK_COUNT -> new OrderSpecifier<?>[]{productOption.stockCnt.sumLong().desc(), product.id.desc()};
            default -> new OrderSpecifier<?>[]{product.crtDtm.desc(), product.id.desc()};
        };
    }
}
