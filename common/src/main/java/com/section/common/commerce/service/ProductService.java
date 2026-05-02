package com.section.common.commerce.service;

import com.section.common.commerce.dto.ProductDetailResDto;
import com.section.common.commerce.dto.ProductListQuery;
import com.section.common.commerce.dto.ProductListReqDto;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductStatsDto;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;

    public Page<ProductListResDto> getProductList(ProductListReqDto reqDto, Pageable pageable) {
        return getProductList(reqDto.toQuery(), pageable);
    }

    public Page<ProductListResDto> getProductList(ProductListQuery query, Pageable pageable) {
        return productRepository.getProductList(query, pageable);
    }

    public java.util.List<ProductListResDto> getProductExportList(ProductListReqDto reqDto, int limit) {
        return getProductExportList(reqDto.toQuery(), limit);
    }

    public java.util.List<ProductListResDto> getProductExportList(ProductListQuery query, int limit) {
        return productRepository.getProductExportList(query, limit);
    }

    public ProductStatsDto getProductStats(ProductListReqDto reqDto) {
        return getProductStats(reqDto.toQuery());
    }

    public ProductStatsDto getProductStats(ProductListQuery query) {
        return productRepository.getProductStats(query);
    }

    public ProductDetailResDto getProductDetail(Long productNo) {
        return productRepository.findProductDetail(productNo);
    }
}
