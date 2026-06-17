package com.section.admin.product.service;

import com.section.admin.product.req.ProductBulkDeleteRequest;
import com.section.admin.product.req.ProductBulkDuplicateRequest;
import com.section.admin.product.req.ProductBulkOperateRequest;
import com.section.admin.product.req.ProductFrontDisplayListRequest;
import com.section.admin.product.req.ProductCreateRequest;
import com.section.admin.product.req.ProductFrontDisplaySaveRequest;
import com.section.admin.product.req.ProductHistoryListRequest;
import com.section.admin.product.req.ProductListRequest;
import com.section.admin.product.req.ProductUpdateRequest;
import com.section.admin.product.res.ProductDetailResponse;
import com.section.admin.product.res.ProductFrontDisplayDashboardResponse;
import com.section.admin.product.res.ProductFrontDisplayRankGuideResponse;
import com.section.admin.product.res.ProductFrontDisplayResponse;
import com.section.admin.product.res.ProductFrontDisplayListResponse;
import com.section.admin.product.res.ProductFrontDisplaySummaryResponse;
import com.section.admin.product.res.ProductHistoryListResponse;
import com.section.admin.product.res.ProductListResponse;
import com.section.admin.product.res.ProductHistoryResponse;
import com.section.admin.settings.service.AdminSettingsService;
import com.section.admin.product.support.ProductExportCsvWriter;
import com.section.admin.product.support.ProductExportPolicy;
import com.section.admin.product.support.ProductFrontDisplayExportCsvWriter;
import com.section.admin.product.support.ProductFrontDisplayExportSummary;
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
import com.section.common.commerce.dto.AdminFrontDisplayProductQuery;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.entity.FrontProductDisplay;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.entity.ProductChangeHistory;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.CategoryRepository;
import com.section.common.commerce.repository.FrontProductDisplayRepository;
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

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {
    private static final int FEATURED_RANK_GUIDE_LIMIT = 12;

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductChangeHistoryRepository productChangeHistoryRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final FrontProductDisplayRepository frontProductDisplayRepository;
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
        List<ProductOption> currentOptions = productOptionRepository.findByProductId(product.getId());
        FrontProductDisplay currentDisplay = frontProductDisplayRepository.findByProductNo(product.getId()).orElse(null);
        String updateSummary = buildUpdateSummary(product, currentOptions, currentDisplay, reqDto);
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

        synchronizeFrontDisplayForStatus(product);

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

    public ProductFrontDisplayResponse getFrontDisplay(Long productNo) {
        ensureProductExists(productNo);
        return ProductFrontDisplayResponse.from(
                productNo,
                frontProductDisplayRepository.findByProductNo(productNo).orElse(null)
        );
    }

    public ProductFrontDisplayRankGuideResponse getFrontDisplayRankGuide(Long productNo) {
        if (productNo != null) {
            ensureProductExists(productNo);
        }

        List<Integer> occupiedRanks = frontProductDisplayRepository.findActiveFeaturedRanks(
                        ProductStatus.ACTIVE.name(),
                        productNo
                ).stream()
                .filter(Objects::nonNull)
                .filter(rank -> rank >= 1 && rank < 999)
                .distinct()
                .sorted()
                .toList();

        Set<Integer> occupiedRankSet = Set.copyOf(occupiedRanks);
        List<Integer> availableRanks = IntStream.rangeClosed(1, FEATURED_RANK_GUIDE_LIMIT)
                .filter(rank -> !occupiedRankSet.contains(rank))
                .boxed()
                .toList();

        Integer recommendedRank = availableRanks.isEmpty() ? FEATURED_RANK_GUIDE_LIMIT : availableRanks.getFirst();
        return new ProductFrontDisplayRankGuideResponse(
                FEATURED_RANK_GUIDE_LIMIT,
                recommendedRank,
                occupiedRanks,
                availableRanks
        );
    }

    public ProductFrontDisplayDashboardResponse getFrontDisplayProducts(ProductFrontDisplayListRequest request) {
        AdminFrontDisplayProductQuery query = buildFrontDisplayQuery(request);
        List<ProductFrontDisplayListResponse> items = productRepository.getAdminFrontDisplayProducts(query).stream()
                .map(ProductFrontDisplayListResponse::from)
                .toList();
        return ProductFrontDisplayDashboardResponse.of(
                query,
                ProductFrontDisplaySummaryResponse.from(items, query.lowStockThreshold()),
                items
        );
    }

    public byte[] exportFrontDisplayProductsCsv(ProductFrontDisplayListRequest request) {
        AdminFrontDisplayProductQuery query = buildFrontDisplayQuery(request);
        List<ProductFrontDisplayListResponse> items = productRepository.getAdminFrontDisplayProducts(query).stream()
                .map(ProductFrontDisplayListResponse::from)
                .toList();
        ProductFrontDisplayExportSummary summary = ProductFrontDisplayExportSummary.of(
                query,
                resolveBrandName(query.brandNo()),
                resolveCategoryName(query.categoryNo())
        );
        return ProductFrontDisplayExportCsvWriter.write(summary, items);
    }

    @Transactional
    public ProductFrontDisplayResponse saveFrontDisplay(ProductFrontDisplaySaveRequest request) {
        Product product = ensureProductExists(request.productNo());
        validateFeaturedDisplay(product, request);
        FrontProductDisplay display = frontProductDisplayRepository.findByProductNo(product.getId())
                .orElseGet(() -> FrontProductDisplay.builder().productNo(product.getId()).build());

        display.updateDisplay(
                request.normalizedHeadline(),
                request.normalizedDescription(),
                request.normalizedMood(),
                request.normalizedFeaturedYn(),
                request.normalizedFeaturedRank()
        );
        FrontProductDisplay saved = frontProductDisplayRepository.save(display);

        recordProductHistory(
                product.getId(),
                ProductHistoryActionType.UPDATED,
                buildFrontDisplayHistorySummary(saved),
                product.getStatus(),
                0,
                0L
        );
        return ProductFrontDisplayResponse.from(product.getId(), saved);
    }

    @Transactional
    public void clearFrontDisplay(Long productNo) {
        Product product = ensureProductExists(productNo);
        frontProductDisplayRepository.findByProductNo(product.getId())
                .ifPresent(display -> {
                    frontProductDisplayRepository.delete(display);
                    recordProductHistory(
                            product.getId(),
                            ProductHistoryActionType.UPDATED,
                            "프론트 노출 정보가 초기화되었습니다.",
                            product.getStatus(),
                            0,
                            0L
                    );
                });
    }

    @Transactional
    public void deleteProduct(Long productNo) {
        Product product = productRepository.findById(productNo)
                .filter(p -> !ProductStatus.DELETE.name().equals(p.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.deleteProduct();
        clearFrontDisplayMetadata(product.getId());
        recordProductHistory(productNo, ProductHistoryActionType.DELETED, "상품이 삭제 처리되었습니다.", ProductStatus.DELETE.name(), 0, 0L);
        log.info("상품 번호 {} 가 성공적으로 논리 삭제되었습니다.", productNo);
    }

    @Transactional
    public BulkOperateResult bulkOperateProducts(ProductBulkOperateRequest reqDto) {
        List<Long> targetProductNos = reqDto.normalizedProductNos();
        ProductStatus targetStatus = reqDto.normalizedStatus();

        List<Product> products = productRepository.findAllById(targetProductNos);
        if (products.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        int updatedCount = 0;
        int unchangedCount = 0;
        int blockedCount = 0;
        for (Product product : products) {
            if (ProductStatus.DELETE.name().equals(product.getStatus())) {
                blockedCount += 1;
                continue;
            }
            if (targetStatus.name().equals(product.getStatus())) {
                unchangedCount += 1;
                continue;
            }

            product.changeStatus(targetStatus);
            synchronizeFrontDisplayForStatus(product);
            recordProductHistory(
                    product.getId(),
                    ProductHistoryActionType.UPDATED,
                    "상품 상태가 일괄 변경되었습니다. 변경 상태: " + targetStatus.name(),
                    targetStatus.name(),
                    0,
                    0L
            );
            updatedCount += 1;
        }

        HashSet<Long> existingProductNoSet = new HashSet<>(products.stream()
                .map(Product::getId)
                .toList());
        long missingCount = targetProductNos.stream()
                .filter(no -> !existingProductNoSet.contains(no))
                .count();

        return new BulkOperateResult(targetProductNos.size(), updatedCount, unchangedCount, blockedCount, (int) missingCount);
    }

    @Transactional
    public BulkDeleteResult bulkDeleteProducts(ProductBulkDeleteRequest reqDto) {
        List<Long> targetProductNos = reqDto.normalizedProductNos();
        List<Product> products = productRepository.findAllById(targetProductNos);
        if (products.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        int deletedCount = 0;
        int alreadyDeletedCount = 0;
        for (Product product : products) {
            if (ProductStatus.DELETE.name().equals(product.getStatus())) {
                alreadyDeletedCount += 1;
                continue;
            }

            product.deleteProduct();
            clearFrontDisplayMetadata(product.getId());
            recordProductHistory(
                    product.getId(),
                    ProductHistoryActionType.DELETED,
                    "상품이 일괄 삭제 처리되었습니다.",
                    ProductStatus.DELETE.name(),
                    0,
                    0L
            );
            deletedCount += 1;
        }

        HashSet<Long> existingProductNoSet = new HashSet<>(products.stream()
                .map(Product::getId)
                .toList());
        long missingCount = targetProductNos.stream()
                .filter(no -> !existingProductNoSet.contains(no))
                .count();

        return new BulkDeleteResult(targetProductNos.size(), deletedCount, alreadyDeletedCount, (int) missingCount);
    }

    @Transactional
    public BulkDuplicateResult bulkDuplicateProducts(ProductBulkDuplicateRequest reqDto) {
        List<Long> targetProductNos = reqDto.normalizedProductNos();
        List<Product> products = productRepository.findAllById(targetProductNos);
        if (products.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        java.util.Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        int blockedCount = 0;
        int missingCount = 0;
        List<Long> createdProductNos = new java.util.ArrayList<>();
        // 일괄 복제 결과는 사용자가 고른 순서와 동일해야 화면 선택/후속 이동 흐름이 어긋나지 않습니다.
        for (Long productNo : targetProductNos) {
            Product product = productMap.get(productNo);
            if (product == null) {
                missingCount += 1;
                continue;
            }
            if (ProductStatus.DELETE.name().equals(product.getStatus())) {
                blockedCount += 1;
                continue;
            }
            createdProductNos.add(duplicateProduct(product).getId());
        }

        return new BulkDuplicateResult(
                targetProductNos.size(),
                createdProductNos.size(),
                blockedCount,
                missingCount,
                createdProductNos
        );
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
                .filter(product -> !ProductStatus.DELETE.name().equals(product.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return duplicateProduct(source).getId();
    }

    private void validateBrandAndCategory(Long brandNo, Long categoryNo) {
        Brand brand = brandRepository.findById(brandNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        Category category = categoryRepository.findById(categoryNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        if (!"Y".equals(brand.getIsActive()) || !"Y".equals(category.getIsActive())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        // 상품은 운영 화면의 선택 구조와 동일하게 최하위 활성 카테고리에만 연결되도록 강제합니다.
        if (category.getParentNo() == null || categoryRepository.existsByParentNo(categoryNo)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Category parentCategory = categoryRepository.findById(category.getParentNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        if (!"Y".equals(parentCategory.getIsActive())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private long resolveLowStockDefaultThreshold() {
        return adminSettingsService.getLowStockDefaultThreshold();
    }

    private AdminFrontDisplayProductQuery buildFrontDisplayQuery(ProductFrontDisplayListRequest request) {
        return new AdminFrontDisplayProductQuery(
                request.normalizedKeyword(),
                request.normalizedStatus(),
                request.normalizedBrandNo(),
                request.normalizedCategoryNo(),
                request.normalizedConfigured(),
                request.normalizedContentStatus(),
                request.normalizedFeaturedOnly(),
                request.normalizedLowStockOnly(),
                request.normalizedLowStockThreshold(resolveLowStockDefaultThreshold()),
                request.normalizedSort()
        );
    }

    private ProductStatus parseProductStatus(String status) {
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private ProductStatus normalizeProductStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return parseProductStatus(status);
    }

    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private Product ensureProductExists(Long productNo) {
        return productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validateFeaturedDisplay(Product product, ProductFrontDisplaySaveRequest request) {
        if (!Boolean.TRUE.equals(request.featured())) {
            return;
        }
        if (request.featuredRank() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!ProductStatus.ACTIVE.name().equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (frontProductDisplayRepository.existsFeaturedRankConflict(
                "Y",
                request.normalizedFeaturedRank(),
                product.getId(),
                ProductStatus.ACTIVE.name()
        )) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String buildFrontDisplayHistorySummary(FrontProductDisplay display) {
        return "프론트 노출 정보가 수정되었습니다. headline="
                + display.getHeadline()
                + ", featured="
                + display.getFeaturedYn()
                + ", rank="
                + display.getFeaturedRank();
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

    private Product duplicateProduct(Product source) {
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

        List<ProductOption> sourceOptions = productOptionRepository.findByProductId(source.getId());
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

        boolean displayCopied = copyFrontDisplayDraft(source.getId(), savedProduct.getId());

        recordProductHistory(
                savedProduct.getId(),
                ProductHistoryActionType.CREATED,
                buildCloneHistorySummary(source.getId(), displayCopied),
                ProductStatus.HIDDEN.name(),
                sourceOptions.size(),
                sourceOptions.stream()
                        .map(ProductOption::getStockCnt)
                        .filter(java.util.Objects::nonNull)
                        .mapToLong(Integer::longValue)
                        .sum()
        );
        return savedProduct;
    }

    private boolean copyFrontDisplayDraft(Long sourceProductNo, Long targetProductNo) {
        return frontProductDisplayRepository.findByProductNo(sourceProductNo)
                .map(display -> {
                    frontProductDisplayRepository.save(FrontProductDisplay.builder()
                            .productNo(targetProductNo)
                            .headline(display.getHeadline())
                            .description(display.getDescription())
                            .mood(display.getMood())
                            .featuredYn("N")
                            .featuredRank(999)
                            .build());
                    return true;
                })
                .orElse(false);
    }

    private String buildCloneHistorySummary(Long sourceProductNo, boolean displayCopied) {
        if (displayCopied) {
            return "상품이 기존 상품에서 복제되었습니다. 원본 상품 번호: "
                    + sourceProductNo
                    + ", 프론트 노출 초안도 함께 복제되었습니다.";
        }
        return "상품이 기존 상품에서 복제되었습니다. 원본 상품 번호: " + sourceProductNo;
    }

    private void synchronizeFrontDisplayForStatus(Product product) {
        if (ProductStatus.ACTIVE.name().equals(product.getStatus())) {
            return;
        }

        frontProductDisplayRepository.findByProductNo(product.getId())
                .ifPresent(display -> {
                    if (ProductStatus.DELETE.name().equals(product.getStatus())) {
                        frontProductDisplayRepository.delete(display);
                        return;
                    }
                    if (!display.isFeatured()) {
                        return;
                    }
                    // 비활성 상품은 대표 진열 순번을 점유하지 않도록 초안만 유지합니다.
                    display.updateDisplay(
                            display.getHeadline(),
                            display.getDescription(),
                            display.getMood(),
                            "N",
                            999
                    );
                    frontProductDisplayRepository.save(display);
                });
    }

    private void clearFrontDisplayMetadata(Long productNo) {
        frontProductDisplayRepository.findByProductNo(productNo)
                .ifPresent(frontProductDisplayRepository::delete);
    }

    private String buildUpdateSummary(
            Product product,
            List<ProductOption> currentOptions,
            FrontProductDisplay currentDisplay,
            ProductUpdateRequest reqDto
    ) {
        List<String> changedFields = new java.util.ArrayList<>();

        if (!product.getCategoryNo().equals(reqDto.getCategoryNo())) changedFields.add("카테고리");
        if (!product.getBrandNo().equals(reqDto.getBrandNo())) changedFields.add("브랜드");
        if (!product.getNameKo().equals(reqDto.normalizeRequiredText(reqDto.getNameKo()))) changedFields.add("상품명");
        if (!java.util.Objects.equals(product.getModelNum(), reqDto.normalizeOptionalText(reqDto.getModelNum()))) changedFields.add("모델번호");
        if (!java.util.Objects.equals(product.getReleasePrice(), reqDto.getReleasePrice())) changedFields.add("발매가");
        if (!java.util.Objects.equals(product.getReleaseDt(), reqDto.getReleaseDt())) changedFields.add("발매일");
        if (!java.util.Objects.equals(product.getThumbnailUrl(), reqDto.normalizeOptionalText(reqDto.getThumbnailUrl()))) changedFields.add("썸네일");
        if (reqDto.getStatus() != null && !product.getStatus().equals(parseProductStatus(reqDto.getStatus()).name())) changedFields.add("상태");
        if (shouldDemoteFeaturedDisplay(product, currentDisplay, reqDto)) changedFields.add("프론트 대표노출");
        if (isOptionChanged(currentOptions, reqDto)) changedFields.add("옵션");

        return changedFields.isEmpty()
                ? "변경된 정보가 없습니다."
                : "변경 항목: " + String.join(", ", changedFields);
    }

    private boolean shouldDemoteFeaturedDisplay(
            Product product,
            FrontProductDisplay currentDisplay,
            ProductUpdateRequest reqDto
    ) {
        if (currentDisplay == null || !currentDisplay.isFeatured() || reqDto.getStatus() == null) {
            return false;
        }
        return ProductStatus.ACTIVE.name().equals(product.getStatus())
                && !ProductStatus.ACTIVE.equals(parseProductStatus(reqDto.getStatus()));
    }

    private boolean isOptionChanged(List<ProductOption> currentOptions, ProductUpdateRequest reqDto) {
        List<String> currentOptionSignatures = (currentOptions == null ? List.<ProductOption>of() : currentOptions).stream()
                .map(this::buildOptionSignature)
                .sorted()
                .toList();
        List<String> requestedOptionSignatures = (reqDto.getOptions() == null ? List.<ProductUpdateRequest.ProductOptionUpdateRequest>of() : reqDto.getOptions()).stream()
                .filter(option -> option.getOptionName() != null && !option.getOptionName().isBlank())
                .map(this::buildOptionSignature)
                .sorted()
                .toList();
        return !currentOptionSignatures.equals(requestedOptionSignatures);
    }

    private String buildOptionSignature(ProductOption option) {
        return String.join("|",
                option.getOptionName(),
                String.valueOf(option.getStockCnt()),
                String.valueOf(option.getAdditionalPrice()));
    }

    private String buildOptionSignature(ProductUpdateRequest.ProductOptionUpdateRequest option) {
        return String.join("|",
                option.normalizeOptionName(),
                String.valueOf(option.getStockCnt()),
                String.valueOf(option.getAdditionalPrice()));
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

    public record BulkOperateResult(
            int requestedCount,
            int updatedCount,
            int unchangedCount,
            int blockedCount,
            int missingCount
    ) {
    }

    public record BulkDeleteResult(
            int requestedCount,
            int deletedCount,
            int alreadyDeletedCount,
            int missingCount
    ) {
    }

    public record BulkDuplicateResult(
            int requestedCount,
            int createdCount,
            int blockedCount,
            int missingCount,
            List<Long> createdProductNos
    ) {
    }
}
