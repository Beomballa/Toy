package com.section.admin.product.service;

import com.section.admin.product.res.ProductDefaultResDto;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public ProductDefaultResDto getProductDefaultInfo() {

        // 1. 모든 브랜드 조회
        List<Brand> brandList = brandRepository.findAll();
        List<ProductDefaultResDto.BrandSimpleDto> brandDtos = brandList.stream()
                .map(ProductDefaultResDto.BrandSimpleDto::from)
                .collect(Collectors.toList());

        // 2. 활성화된 카테고리만 조회 (isActive = 'Y')
        List<Category> categoryList = categoryRepository.findAll().stream()
                .filter(category -> "Y".equals(category.getIsActive()))
                .collect(Collectors.toList());

        List<ProductDefaultResDto.CategorySimpleDto> categoryDtos = categoryList.stream()
                .map(ProductDefaultResDto.CategorySimpleDto::from)
                .collect(Collectors.toList());

        // 3. DTO 생성 및 반환
        return ProductDefaultResDto.builder()
                .brands(brandDtos)
                .categories(categoryDtos)
                .build();
    }

    /**
     * 1depth 카테고리만 조회 (최상위 카테고리)
     */
    public List<ProductDefaultResDto.CategorySimpleDto> getTopLevelCategories() {
        return categoryRepository.findByDepth(1).stream()
                .filter(category -> "Y".equals(category.getIsActive()))
                .map(ProductDefaultResDto.CategorySimpleDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 부모의 하위 카테고리 조회 (2depth)
     */
    public List<ProductDefaultResDto.CategorySimpleDto> getSubCategories(Long parentNo) {
        return categoryRepository.findByParentNo(parentNo).stream()
                .filter(category -> "Y".equals(category.getIsActive()))
                .map(ProductDefaultResDto.CategorySimpleDto::from)
                .collect(Collectors.toList());
    }

}
