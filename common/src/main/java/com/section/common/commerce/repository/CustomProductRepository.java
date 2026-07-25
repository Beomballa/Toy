package com.section.common.commerce.repository;

import com.section.common.commerce.dto.AdminFrontDisplayProductQuery;
import com.section.common.commerce.dto.AdminFrontDisplayProductRow;
import com.section.common.commerce.dto.FrontCatalogProductRow;
import com.section.common.commerce.dto.FrontCatalogQuery;
import com.section.common.commerce.dto.FrontCatalogSummaryRow;
import com.section.common.commerce.dto.ProductListQuery;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductStatsDto;
import com.section.common.base.entity.type.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomProductRepository {

    Page<ProductListResDto> getProductList(ProductListQuery query, Pageable pageable);

    List<ProductListResDto> getProductExportList(ProductListQuery query, int limit);

    ProductStatsDto getProductStats(ProductListQuery query);

    List<ProductListResDto> getLowStockProducts(int threshold, int limit);

    List<Long> getReferencedBrandNos(Collection<Long> brandNos);

    List<Long> getReferencedCategoryNos(Collection<Long> categoryNos);

    Page<FrontCatalogProductRow> getFrontCatalogProducts(FrontCatalogQuery query, Pageable pageable);

    List<FrontCatalogProductRow> getFrontCatalogPreviewProducts(FrontCatalogQuery query, int limit);

    List<FrontCatalogSummaryRow> getFrontCatalogSummary(FrontCatalogQuery query);

    Optional<FrontCatalogProductRow> getFrontCatalogProduct(Long productNo);

    List<FrontCatalogProductRow> getRelatedFrontCatalogProducts(Long productNo, Long brandNo, Long categoryNo, int limit);

    List<AdminFrontDisplayProductRow> getAdminFrontDisplayProducts(AdminFrontDisplayProductQuery query);
}
