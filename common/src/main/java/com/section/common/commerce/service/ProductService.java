package com.section.common.commerce.service;

import com.section.common.commerce.dto.ProductListReqDto;
import com.section.common.commerce.dto.ProductListResDto;
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
        return productRepository.getProductList(reqDto, pageable);
    }
}
