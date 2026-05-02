package com.section.admin.product.service;

import com.section.admin.product.req.ProductCreateRequest;
import com.section.admin.product.req.ProductListRequest;
import com.section.admin.product.req.ProductUpdateRequest;
import com.section.admin.product.res.ProductDetailResponse;
import com.section.admin.product.res.ProductListResponse;
import com.section.admin.product.support.ProductListPagePolicy;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.admin.product.res.ProductDefaultResDto;
import com.section.common.commerce.dto.ProductDetailResDto;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductStatsDto;
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
import java.util.Optional;
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
    public ProductListResponse getProductList(ProductListRequest req, Pageable pageable) {
        Pageable normalizedPageable = ProductListPagePolicy.normalize(pageable);
        Page<ProductListResDto> resDto = productService.getProductList(req.toProductListReqDto(), normalizedPageable);
        ProductStatsDto statsDto = productService.getProductStats(req.toProductListReqDto());

        Page<ProductListResponse.ProductListItem> result = resDto.map(ProductListResponse.ProductListItem::from);
        ProductListResponse.ProductStatsItem stats = ProductListResponse.ProductStatsItem.from(statsDto);
        return ProductListResponse.of(result, stats);
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
        return new ProductDefaultResDto(brandDtos, categoryDtos);
    }

    /**
     * 새로운 상품 등록
     * @Param reqDto
     * */
    @Transactional
    public Long createProductInfo(ProductCreateRequest reqDto) {
        validateBrandAndCategory(reqDto.getBrandNo(), reqDto.getCategoryNo());
        // 상품 정보 저장
        Product product = Product.createProduct(reqDto.toProductCreateReqDto());
        Product savedProduct = productRepository.save(product);

        // 상품 옵션(ProductOption) 저장
        if (reqDto.getOptions() != null && !reqDto.getOptions().isEmpty()) {
            List<ProductOption> productOptions = reqDto.getOptions().stream()
                    .map(optDto -> ProductOption.builder()
                            .productNo(savedProduct.getId())
                            .optionName(optDto.normalizeOptionName())
                            .stockCnt(optDto.getStockCnt())
                            .additionalPrice(optDto.getAdditionalPrice())
                            .build())
                    .collect(Collectors.toList());

            productOptionRepository.saveAll(productOptions);
        }

        return savedProduct.getId();
    }

    /**
     * 상품 정보 수정
     * */
    @Transactional
    public void updateProductInfo(ProductUpdateRequest reqDto) {
        Product product = productRepository.findById(reqDto.getProductNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        validateBrandAndCategory(reqDto.getBrandNo(), reqDto.getCategoryNo());

        // 기본 정보 수정
        product.updateBasicInfo(
                reqDto.normalizeRequiredText(reqDto.getNameKo()),
                reqDto.normalizeOptionalText(reqDto.getModelNum()),
                reqDto.getReleasePrice(),
                reqDto.getReleaseDt(),
                reqDto.normalizeOptionalText(reqDto.getThumbnailUrl())
        );
        product.changeCategory(reqDto.getCategoryNo());
        product.changeBrand(reqDto.getBrandNo());
        if (reqDto.getStatus() != null) {
            product.changeStatus(parseProductStatus(reqDto.getStatus()));
        }

        // 옵션 수정: 기존 옵션 삭제 후 재등록
        productOptionRepository.deleteByProductNo(product.getId());

        if (reqDto.getOptions() != null && !reqDto.getOptions().isEmpty()) {
            List<ProductOption> productOptions = reqDto.getOptions().stream()
                    .filter(opt -> opt.getOptionName() != null && !opt.getOptionName().isBlank())
                    .map(optDto -> ProductOption.builder()
                            .productNo(product.getId())
                            .optionName(optDto.normalizeOptionName())
                            .stockCnt(optDto.getStockCnt())
                            .additionalPrice(optDto.getAdditionalPrice())
                            .build())
                    .toList();

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

    /**
     * 상품 상세정보 조회
     * */
    public ProductDetailResponse getProductDetail(Long productNo) {
        ProductDetailResDto resDto = productService.getProductDetail(productNo);
        if (resDto == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        List<ProductOption> option = productOptionRepository.findByProductId(productNo);
        return ProductDetailResponse.from(resDto, option);
    }

    @Transactional
    public void deleteProduct(Long productNo) {
        Product product = productRepository.findById(productNo)
                .filter(p -> !ProductStatus.DELETE.name().equals(p.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.deleteProduct();
        log.info("상품 번호 {} 가 성공적으로 논리 삭제되었습니다.", productNo);
    }

    private void validateBrandAndCategory(Long brandNo, Long categoryNo) {
        if (!brandRepository.existsById(brandNo) || !categoryRepository.existsById(categoryNo)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private ProductStatus parseProductStatus(String status) {
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
