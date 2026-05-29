package com.section.admin.product.service;

import com.section.admin.product.req.ProductCreateRequest;
import com.section.admin.product.req.ProductHistoryListRequest;
import com.section.admin.product.req.ProductListRequest;
import com.section.admin.product.req.ProductUpdateRequest;
import com.section.admin.product.res.ProductDetailResponse;
import com.section.admin.product.res.ProductHistoryListResponse;
import com.section.admin.product.res.ProductListResponse;
import com.section.admin.product.res.ProductHistoryResponse;
import com.section.admin.settings.service.AdminSettingsService;
import com.section.admin.product.support.ProductExportCsvWriter;
import com.section.admin.product.support.ProductExportPolicy;
import com.section.admin.product.support.ProductExportSummary;
import com.section.admin.product.support.ProductListPagePolicy;
import com.section.common.base.entity.type.ProductHistoryActionType;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.admin.product.res.ProductDefaultResDto;
import com.section.common.commerce.dto.ProductDetailResDto;
import com.section.common.commerce.dto.ProductHistoryListQuery;
import com.section.common.commerce.dto.ProductHistoryListResDto;
import com.section.common.commerce.dto.ProductListQuery;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductStatsDto;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.entity.ProductChangeHistory;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.CategoryRepository;
import com.section.common.commerce.repository.ProductChangeHistoryRepository;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.commerce.service.ProductService;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductChangeHistoryRepository productChangeHistoryRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminSettingsService adminSettingsService;

    private final ProductService productService;

    /**
     * 등록된 카테고리, 브랜드 조회 용도
     * @return ProductDefaultResDto
     * */
    public ProductListResponse getProductList(ProductListRequest req, Pageable pageable) {
        Pageable normalizedPageable = ProductListPagePolicy.normalize(pageable);
        ProductListQuery query = req.toProductListReqDto().toQuery(resolveLowStockDefaultThreshold());
        ProductListQuery statsQuery = query.toStatsQuery();
        Page<ProductListResDto> resDto = productService.getProductList(query, normalizedPageable);
        ProductStatsDto statsDto = productService.getProductStats(statsQuery);

        Page<ProductListResponse.ProductListItem> result = resDto.map(ProductListResponse.ProductListItem::from);
        ProductListResponse.ProductStatsItem stats =
                ProductListResponse.ProductStatsItem.from(statsDto, query, statsQuery);
        ProductListResponse.ResultMetaItem resultMeta =
                ProductListResponse.ResultMetaItem.from(query, result);
        return ProductListResponse.of(result, stats, ProductListResponse.AppliedQueryItem.from(query), resultMeta);
    }

    public byte[] exportProductListCsv(ProductListRequest req) {
        ProductListQuery query = req.toProductListReqDto().toQuery(resolveLowStockDefaultThreshold());
        List<ProductListResDto> result = productService.getProductExportList(
                query,
                ProductExportPolicy.MAX_EXPORT_SIZE
        );
        ProductExportSummary summary = ProductExportSummary.from(
                query,
                resolveBrandName(query.brandNo()),
                resolveCategoryName(query.categoryNo())
        );

        return ProductExportCsvWriter.write(summary, result);
    }

    public long getLowStockDefaultThreshold() {
        return resolveLowStockDefaultThreshold();
    }

    /**
     * 등록된 카테고리, 브랜드 조회 용도
     * @return ProductDefaultResDto
     * */
    public ProductDefaultResDto getProductDefaultInfo() {
        List<Brand> brandList = brandRepository.findByIsActiveOrderByNameKoAsc("Y");
        List<ProductDefaultResDto.BrandSimpleDto> brandDtos = brandList.stream()
                .map(ProductDefaultResDto.BrandSimpleDto::from)
                .collect(Collectors.toList());

        List<Category> categoryList = categoryRepository.findByIsActiveOrderByDepthAscNameAscCategoryNoAsc("Y")
                .stream()
                .sorted(Comparator
                        .comparing(Category::getDepth)
                        .thenComparing(category -> category.getParentNo() == null ? 0L : category.getParentNo())
                        .thenComparing(Category::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Category::getCategoryNo))
                .toList();

        List<ProductDefaultResDto.CategorySimpleDto> categoryDtos = categoryList.stream()
                .map(ProductDefaultResDto.CategorySimpleDto::from)
                .collect(Collectors.toList());

        return new ProductDefaultResDto(brandDtos, categoryDtos);
    }

    /**
     * 새로운 상품 등록
     * @Param reqDto
     * */
    @Transactional
    public Long createProductInfo(ProductCreateRequest reqDto) {
        validateBrandAndCategory(reqDto.getBrandNo(), reqDto.getCategoryNo());
        validateDuplicateOptionNames(reqDto.getOptions() == null ? List.of() :
                reqDto.getOptions().stream()
                        .map(ProductCreateRequest.ProductOptionRequest::normalizeOptionName)
                        .toList());
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

        recordProductHistory(
                savedProduct.getId(),
                ProductHistoryActionType.CREATED,
                "상품이 새로 등록되었습니다.",
                ProductStatus.ACTIVE.name(),
                reqDto.getOptions() == null ? 0 : reqDto.getOptions().size(),
                reqDto.getOptions() == null ? 0L : reqDto.getOptions().stream()
                        .map(ProductCreateRequest.ProductOptionRequest::getStockCnt)
                        .filter(java.util.Objects::nonNull)
                        .mapToLong(Integer::longValue)
                        .sum()
        );

        return savedProduct.getId();
    }

    /**
     * 상품 정보 수정
     * */
    @Transactional
    public void updateProductInfo(ProductUpdateRequest reqDto) {
        Product product = productRepository.findById(reqDto.getProductNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        String updateSummary = buildUpdateSummary(product, reqDto);
        validateBrandAndCategory(reqDto.getBrandNo(), reqDto.getCategoryNo());
        validateDuplicateOptionNames(reqDto.getOptions() == null ? List.of() :
                reqDto.getOptions().stream()
                        .map(ProductUpdateRequest.ProductOptionUpdateRequest::normalizeOptionName)
                        .toList());

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

        recordProductHistory(
                product.getId(),
                ProductHistoryActionType.UPDATED,
                updateSummary,
                product.getStatus(),
                reqDto.getOptions() == null ? 0 : reqDto.getOptions().size(),
                reqDto.getOptions() == null ? 0L : reqDto.getOptions().stream()
                        .map(ProductUpdateRequest.ProductOptionUpdateRequest::getStockCnt)
                        .filter(java.util.Objects::nonNull)
                        .mapToLong(Integer::longValue)
                        .sum()
        );
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
        recordProductHistory(productNo, ProductHistoryActionType.DELETED, "상품이 삭제 처리되었습니다.", ProductStatus.DELETE.name(), 0, 0L);
        log.info("상품 번호 {} 가 성공적으로 논리 삭제되었습니다.", productNo);
    }

    public List<ProductHistoryResponse> getProductHistory(Long productNo) {
        if (!productRepository.existsById(productNo)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        List<ProductChangeHistory> histories = productChangeHistoryRepository.findTop20ByProductNoOrderByHistoryNoDesc(productNo);
        java.util.Map<Long, String> actorNameMap = adminUserRepository.findAllById(
                        histories.stream()
                                .map(ProductChangeHistory::getCrtNo)
                                .filter(java.util.Objects::nonNull)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(AdminUser::getAdminNo, AdminUser::getName));

        return histories.stream()
                .map(history -> ProductHistoryResponse.from(
                        history,
                        actorNameMap.getOrDefault(history.getCrtNo(), history.getCrtNo() == null ? "-" : "관리자#" + history.getCrtNo())
                ))
                .toList();
    }

    public ProductHistoryListResponse getProductHistoryList(ProductHistoryListRequest req, Pageable pageable) {
        ProductHistoryListQuery query = req.toQuery();
        Page<ProductHistoryListResDto> page = productChangeHistoryRepository.getProductHistoryList(query, pageable);
        return ProductHistoryListResponse.of(page, query);
    }

    @Transactional
    public Long cloneProduct(Long productNo) {
        Product source = productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        Product clonedProduct = Product.builder()
                .categoryNo(source.getCategoryNo())
                .brandNo(source.getBrandNo())
                .nameKo(source.getNameKo() + " (복제)")
                .modelNum(source.getModelNum())
                .releasePrice(source.getReleasePrice())
                .releaseDt(source.getReleaseDt())
                .thumbnailUrl(source.getThumbnailUrl())
                .status(ProductStatus.HIDDEN.name())
                .build();
        Product savedProduct = productRepository.save(clonedProduct);

        List<ProductOption> sourceOptions = productOptionRepository.findByProductId(productNo);
        if (!sourceOptions.isEmpty()) {
            productOptionRepository.saveAll(sourceOptions.stream()
                    .map(option -> ProductOption.builder()
                            .productNo(savedProduct.getId())
                            .optionName(option.getOptionName())
                            .stockCnt(option.getStockCnt())
                            .additionalPrice(option.getAdditionalPrice())
                            .build())
                    .toList());
        }

        recordProductHistory(
                savedProduct.getId(),
                ProductHistoryActionType.CREATED,
                "상품이 기존 상품에서 복제되었습니다. 원본 상품 번호: " + productNo,
                ProductStatus.HIDDEN.name(),
                sourceOptions.size(),
                sourceOptions.stream()
                        .map(ProductOption::getStockCnt)
                        .filter(java.util.Objects::nonNull)
                        .mapToLong(Integer::longValue)
                        .sum()
        );
        return savedProduct.getId();
    }

    private void validateBrandAndCategory(Long brandNo, Long categoryNo) {
        if (!brandRepository.existsById(brandNo) || !categoryRepository.existsById(categoryNo)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private long resolveLowStockDefaultThreshold() {
        return adminSettingsService.getLowStockDefaultThreshold();
    }

    private ProductStatus parseProductStatus(String status) {
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateDuplicateOptionNames(List<String> optionNames) {
        // 옵션명은 저장 전 공백 정규화 기준으로 중복 여부를 판단해야 화면 표시와 DB 값이 어긋나지 않습니다.
        Set<String> distinctNames = optionNames.stream().collect(Collectors.toSet());
        if (distinctNames.size() != optionNames.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void recordProductHistory(
            Long productNo,
            ProductHistoryActionType actionType,
            String summary,
            String statusSnapshot,
            int optionCount,
            long totalStock
    ) {
        productChangeHistoryRepository.save(
                ProductChangeHistory.of(productNo, actionType, summary, statusSnapshot, optionCount, totalStock)
        );
    }

    private String buildUpdateSummary(Product product, ProductUpdateRequest reqDto) {
        List<String> changedFields = new java.util.ArrayList<>();

        if (!product.getCategoryNo().equals(reqDto.getCategoryNo())) changedFields.add("카테고리");
        if (!product.getBrandNo().equals(reqDto.getBrandNo())) changedFields.add("브랜드");
        if (!product.getNameKo().equals(reqDto.normalizeRequiredText(reqDto.getNameKo()))) changedFields.add("상품명");
        if (!java.util.Objects.equals(product.getModelNum(), reqDto.normalizeOptionalText(reqDto.getModelNum()))) changedFields.add("모델번호");
        if (!java.util.Objects.equals(product.getReleasePrice(), reqDto.getReleasePrice())) changedFields.add("발매가");
        if (!java.util.Objects.equals(product.getReleaseDt(), reqDto.getReleaseDt())) changedFields.add("발매일");
        if (!java.util.Objects.equals(product.getThumbnailUrl(), reqDto.normalizeOptionalText(reqDto.getThumbnailUrl()))) changedFields.add("썸네일");
        if (reqDto.getStatus() != null && !product.getStatus().equals(parseProductStatus(reqDto.getStatus()).name())) changedFields.add("상태");
        changedFields.add("옵션");

        return changedFields.isEmpty()
                ? "변경된 정보가 없습니다."
                : "변경 항목: " + String.join(", ", changedFields);
    }

    private String resolveBrandName(Long brandNo) {
        if (brandNo == null) {
            return null;
        }

        return brandRepository.findById(brandNo)
                .map(Brand::getNameKo)
                .orElse(null);
    }

    private String resolveCategoryName(Long categoryNo) {
        if (categoryNo == null) {
            return null;
        }

        return categoryRepository.findById(categoryNo)
                .map(Category::getName)
                .orElse(null);
    }
}
