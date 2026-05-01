package com.section.common.commerce.repository;

import com.section.common.commerce.dto.ProductListQuery;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductStatsDto;
import com.section.common.commerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomProductRepository {

    Page<ProductListResDto> getProductList(ProductListQuery query, Pageable pageable);

    ProductStatsDto getProductStats(ProductListQuery query);

    List<ProductListResDto> getLowStockProducts(int threshold, int limit);
}
