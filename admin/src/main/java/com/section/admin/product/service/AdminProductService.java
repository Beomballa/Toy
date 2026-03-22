package com.section.admin.product.service;

import com.section.admin.product.req.ProductCreateRequest;
import com.section.admin.product.req.ProductListRequest;
import com.section.admin.product.res.ProductListResponse;
import com.section.common.commerce.dto.ProductCreateReqDto;
import com.section.admin.product.res.ProductDefaultResDto;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.CategoryRepository;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.commerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    private final ProductService productService;

    /**
     * 등록된 카테고리, 브랜드 조회 용도
     * @return ProductDefaultResDto
     * */
    public Page<ProductListResponse.ProductListItem> getProductList(ProductListRequest req, Pageable pageable) {
        Page<ProductListResDto> resDto = productService.getProductList(req.toProductListReqDto(), pageable);
        return resDto.map(ProductListResponse.ProductListItem::from);
    }

    /**
     * 등록된 카테고리, 브랜드 조회 용도
     * @return ProductDefaultResDto
     * */
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

    @Transactional
    public void createProductInfo(ProductCreateRequest reqDto) {

        if(!brandRepository.existsById(reqDto.getBrandNo())){
            throw new IllegalArgumentException("존재하지 않는 브랜드입니다. brandNo : " + reqDto.getBrandNo());
        }
        if(!categoryRepository.existsById(reqDto.getCategoryNo())){
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다. categoryNo : " + reqDto.getCategoryNo());
        }
        // 상품 정보 저장
        Product product = Product.createProduct(reqDto.toProductCreateReqDto());
        Product savedProduct = productRepository.save(product);

        // 상품 옵션(ProductOption) 저장
        if (reqDto.getOptions() != null && !reqDto.getOptions().isEmpty()) {
            List<ProductOption> productOptions = reqDto.getOptions().stream()
                    .map(optDto -> ProductOption.builder()
                            .productNo(savedProduct.getId())
                            .optionName(optDto.getOptionName())
                            .stockCnt(optDto.getStockCnt())
                            .build())
                    .collect(Collectors.toList());

            productOptionRepository.saveAll(productOptions);
        }
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
