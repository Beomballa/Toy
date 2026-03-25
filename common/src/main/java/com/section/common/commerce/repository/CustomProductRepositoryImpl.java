package com.section.common.commerce.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.ProductListReqDto;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

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
    public Page<ProductListResDto> getProductList(ProductListReqDto reqDto, Pageable pageable) {
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
                                productOption.stockCnt.sumLong().as("totalStock"),
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
                        searchKeywordLike(reqDto.getSearchKeyword()),
                        categoryNoEq(reqDto.getCategoryNo()),
                        brandNoEq(reqDto.getBrandNo()),
                        isActiveEq()
                )
                .orderBy(product.releaseDt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .leftJoin(brand).on(brand.brandNo.eq(product.brandNo))
                .leftJoin(category).on(category.categoryNo.eq(product.categoryNo))
                .leftJoin(productOption).on(productOption.productNo.eq(product.id))
                .where(
                        searchKeywordLike(reqDto.getSearchKeyword()),
                        categoryNoEq(reqDto.getCategoryNo()),
                        brandNoEq(reqDto.getBrandNo()),
                        isActiveEq()
                );

        return PageableExecutionUtils.getPage(list, pageable, countQuery::fetchOne);
    }

    public BooleanExpression searchKeywordLike(String searchKeyword) {
        return product.nameKo.containsIgnoreCase(searchKeyword)
                .or(product.modelNum.containsIgnoreCase(searchKeyword));
    }

    public BooleanExpression categoryNoEq(Long categoryNo) {
        if(categoryNo == null) {
            return null;
        }
        return product.categoryNo.eq(categoryNo);
    }

    public BooleanExpression brandNoEq(Long brandNo) {
        if(brandNo == null) {
            return null;
        }
        return product.brandNo.eq(brandNo);
    }

    public BooleanExpression isActiveEq() {
        return product.status.ne(ProductStatus.DELETE.name());
    }
}
