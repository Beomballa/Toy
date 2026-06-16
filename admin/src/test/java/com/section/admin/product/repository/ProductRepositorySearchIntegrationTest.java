package com.section.admin.product.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.base.entity.type.ProductOrderType;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.AdminFrontDisplayProductQuery;
import com.section.common.commerce.dto.AdminFrontDisplayProductRow;
import com.section.common.commerce.dto.FrontCatalogProductRow;
import com.section.common.commerce.dto.FrontCatalogQuery;
import com.section.common.commerce.dto.ProductListQuery;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductStatsDto;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.entity.FrontProductDisplay;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.CategoryRepository;
import com.section.common.commerce.repository.FrontProductDisplayRepository;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class ProductRepositorySearchIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private FrontProductDisplayRepository frontProductDisplayRepository;

    @Test
    @DisplayName("상품 목록 검색은 브랜드명과 카테고리명까지 QueryDSL 조건으로 조회한다")
    void getProductListSearchesBrandAndCategoryNames() {
        Brand searchBrand = brandRepository.save(Brand.builder()
                .nameKo("테스트 브랜드 검색")
                .nameEn("Test Brand Search")
                .isActive("Y")
                .build());
        Category searchCategory = categoryRepository.save(Category.builder()
                .name("테스트 카테고리 검색")
                .depth(1)
                .isActive("Y")
                .build());
        Product searchProduct = productRepository.save(Product.builder()
                .brandNo(searchBrand.getBrandNo())
                .categoryNo(searchCategory.getCategoryNo())
                .nameKo("검색 대상 상품")
                .modelNum("SEARCH-001")
                .releasePrice(1000)
                .releaseDt(LocalDate.of(2026, 5, 3))
                .status(ProductStatus.ACTIVE.name())
                .build());

        Brand otherBrand = brandRepository.save(Brand.builder()
                .nameKo("일반 브랜드")
                .nameEn("General Brand")
                .isActive("Y")
                .build());
        Category otherCategory = categoryRepository.save(Category.builder()
                .name("일반 카테고리")
                .depth(1)
                .isActive("Y")
                .build());
        productRepository.save(Product.builder()
                .brandNo(otherBrand.getBrandNo())
                .categoryNo(otherCategory.getCategoryNo())
                .nameKo("일반 상품")
                .modelNum("NORMAL-001")
                .releasePrice(1000)
                .releaseDt(LocalDate.of(2026, 5, 3))
                .status(ProductStatus.ACTIVE.name())
                .build());

        Page<ProductListResDto> brandResult = productRepository.getProductList(
                new ProductListQuery(null, null, null, "테스트 브랜드 검색", ProductOrderType.RECENT, false, null, false),
                PageRequest.of(0, 10)
        );
        Page<ProductListResDto> categoryResult = productRepository.getProductList(
                new ProductListQuery(null, null, null, "테스트 카테고리 검색", ProductOrderType.RECENT, false, null, false),
                PageRequest.of(0, 10)
        );
        ProductStatsDto stats = productRepository.getProductStats(
                new ProductListQuery(null, null, null, "테스트 브랜드 검색", ProductOrderType.RECENT, false, null, false)
        );

        assertEquals(1, brandResult.getTotalElements());
        assertEquals(searchProduct.getId(), brandResult.getContent().getFirst().getProductNo());
        assertEquals(1, categoryResult.getTotalElements());
        assertEquals(searchProduct.getId(), categoryResult.getContent().getFirst().getProductNo());
        assertEquals(1L, stats.getTotalCount());
        assertTrue(brandResult.getContent().stream().allMatch(item -> "검색 대상 상품".equals(item.getProductName())));
    }

    @Test
    @DisplayName("상품 목록 검색은 공백 단위 다중 키워드와 모델번호 정규화 검색을 함께 지원한다")
    void getProductListSupportsTokenizedKeywordAndNormalizedModelSearch() {
        Brand brandEntity = brandRepository.save(Brand.builder()
                .nameKo("아식스 퍼포먼스 ZQX")
                .nameEn("Asics Performance")
                .isActive("Y")
                .build());
        Category categoryEntity = categoryRepository.save(Category.builder()
                .name("러닝화")
                .depth(1)
                .isActive("Y")
                .build());

        Product matchedProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("젤 카야노 14 유니크")
                .modelNum("ZX1201A-019")
                .releasePrice(189000)
                .releaseDt(LocalDate.of(2026, 6, 1))
                .status(ProductStatus.ACTIVE.name())
                .build());

        productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("젤 님버스 27")
                .modelNum("ZX1203A-777")
                .releasePrice(199000)
                .releaseDt(LocalDate.of(2026, 6, 1))
                .status(ProductStatus.ACTIVE.name())
                .build());

        Page<ProductListResDto> result = productRepository.getProductList(
                new ProductListQuery(null, null, null, "ZQX 유니크 ZX1201A019", ProductOrderType.RECENT, false, null, false),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(matchedProduct.getId(), result.getContent().getFirst().getProductNo());
    }

    @Test
    @DisplayName("프론트 노출 관리 검색은 모델번호 구분자 없이도 QueryDSL 정규화 검색을 지원한다")
    void getAdminFrontDisplayProductsSupportsNormalizedModelKeyword() {
        Brand brandEntity = brandRepository.save(Brand.builder()
                .nameKo("아식스 퍼포먼스")
                .nameEn("Asics Performance")
                .isActive("Y")
                .build());
        Category categoryEntity = categoryRepository.save(Category.builder()
                .name("러닝화")
                .depth(1)
                .isActive("Y")
                .build());

        Product matchedProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("젤 카야노 14")
                .modelNum("ZQX1201A-019-UNIQUE")
                .releasePrice(189000)
                .releaseDt(LocalDate.of(2026, 6, 1))
                .status(ProductStatus.ACTIVE.name())
                .build());
        frontProductDisplayRepository.save(FrontProductDisplay.builder()
                .productNo(matchedProduct.getId())
                .headline("Metal calm")
                .description("안정적인 주행감")
                .mood("Refined silver")
                .featuredYn("N")
                .featuredRank(999)
                .build());

        Product otherProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("젤 님버스 27")
                .modelNum("1203A-777")
                .releasePrice(199000)
                .releaseDt(LocalDate.of(2026, 6, 1))
                .status(ProductStatus.ACTIVE.name())
                .build());
        frontProductDisplayRepository.save(FrontProductDisplay.builder()
                .productNo(otherProduct.getId())
                .headline("Cloud flow")
                .description("풍부한 쿠셔닝")
                .mood("Daily comfort")
                .featuredYn("N")
                .featuredRank(999)
                .build());

        List<AdminFrontDisplayProductRow> result = productRepository.getAdminFrontDisplayProducts(
                new AdminFrontDisplayProductQuery("ZQX1201A019UNIQUE", null, null, null, null, null, false, false, 20, "FEATURED")
        );

        assertEquals(1, result.size());
        assertEquals(matchedProduct.getId(), result.getFirst().productNo());
    }

    @Test
    @DisplayName("저재고 카드 통계는 빠른 필터와 분리된 기준 QueryDSL을 사용한다")
    void lowStockThresholdAffectsListAndBaseStats() {
        Brand stockBrand = brandRepository.save(Brand.builder()
                .nameKo("재고 테스트 브랜드")
                .nameEn("Stock Brand")
                .isActive("Y")
                .build());
        Category stockCategory = categoryRepository.save(Category.builder()
                .name("재고 테스트 카테고리")
                .depth(1)
                .isActive("Y")
                .build());

        Product lowStockProduct = productRepository.save(Product.builder()
                .brandNo(stockBrand.getBrandNo())
                .categoryNo(stockCategory.getCategoryNo())
                .nameKo("저재고 상품")
                .modelNum("LOW-STOCK-001")
                .releasePrice(1000)
                .releaseDt(LocalDate.of(2026, 5, 4))
                .status(ProductStatus.ACTIVE.name())
                .build());
        productOptionRepository.save(ProductOption.builder()
                .productNo(lowStockProduct.getId())
                .optionName("260")
                .stockCnt(20)
                .additionalPrice(0)
                .build());

        Product normalStockProduct = productRepository.save(Product.builder()
                .brandNo(stockBrand.getBrandNo())
                .categoryNo(stockCategory.getCategoryNo())
                .nameKo("일반 재고 상품")
                .modelNum("NORMAL-STOCK-001")
                .releasePrice(1000)
                .releaseDt(LocalDate.of(2026, 5, 4))
                .status(ProductStatus.ACTIVE.name())
                .build());
        productOptionRepository.save(ProductOption.builder()
                .productNo(normalStockProduct.getId())
                .optionName("270")
                .stockCnt(80)
                .additionalPrice(0)
                .build());

        ProductListQuery lowStockThresholdQuery =
                new ProductListQuery(null, null, null, "재고 테스트 브랜드", ProductOrderType.RECENT, true, 30L, false);

        Page<ProductListResDto> listResult = productRepository.getProductList(lowStockThresholdQuery, PageRequest.of(0, 10));
        ProductStatsDto stats = productRepository.getProductStats(lowStockThresholdQuery.toStatsQuery());

        assertEquals(1, listResult.getTotalElements());
        assertEquals(lowStockProduct.getId(), listResult.getContent().getFirst().getProductNo());
        assertEquals(2L, stats.getTotalCount());
        assertEquals(2L, stats.getActiveCount());
        assertEquals(1L, stats.getLowStockCount());
    }

    @Test
    @DisplayName("상위 카테고리 필터는 하위 카테고리 상품까지 함께 조회한다")
    void parentCategoryFilterIncludesChildCategoryProducts() {
        Category parentCategory = categoryRepository.save(Category.builder()
                .name("신발")
                .depth(1)
                .isActive("Y")
                .build());
        Category childCategory = categoryRepository.save(Category.builder()
                .parentNo(parentCategory.getCategoryNo())
                .name("러닝화")
                .depth(2)
                .isActive("Y")
                .build());
        Brand brand = brandRepository.save(Brand.builder()
                .nameKo("뉴발란스")
                .nameEn("New Balance")
                .isActive("Y")
                .build());

        Product childCategoryProduct = productRepository.save(Product.builder()
                .brandNo(brand.getBrandNo())
                .categoryNo(childCategory.getCategoryNo())
                .nameKo("1080v14")
                .modelNum("NB-1080")
                .releasePrice(219000)
                .releaseDt(LocalDate.of(2026, 5, 20))
                .status(ProductStatus.ACTIVE.name())
                .build());

        Page<ProductListResDto> result = productRepository.getProductList(
                new ProductListQuery(parentCategory.getCategoryNo(), null, null, null, ProductOrderType.RECENT, false, null, false),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(childCategoryProduct.getId(), result.getContent().getFirst().getProductNo());
    }

    @Test
    @DisplayName("상품 목록 정렬은 동일 값일 때도 최신 상품이 먼저 오도록 안정적으로 유지한다")
    void productListOrderingStaysStableWhenSortValuesTie() {
        Brand brandEntity = brandRepository.save(Brand.builder()
                .nameKo("정렬 테스트 브랜드")
                .nameEn("Sorting Brand")
                .isActive("Y")
                .build());
        Category categoryEntity = categoryRepository.save(Category.builder()
                .name("정렬 테스트 카테고리")
                .depth(1)
                .isActive("Y")
                .build());

        Product firstReleasePriceProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("릴리즈 프라이스 A")
                .modelNum("REL-A")
                .releasePrice(129000)
                .releaseDt(LocalDate.of(2026, 6, 1))
                .status(ProductStatus.ACTIVE.name())
                .build());
        Product secondReleasePriceProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("릴리즈 프라이스 B")
                .modelNum("REL-B")
                .releasePrice(129000)
                .releaseDt(LocalDate.of(2026, 6, 1))
                .status(ProductStatus.ACTIVE.name())
                .build());

        Product firstStockProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("재고 A")
                .modelNum("STOCK-A")
                .releasePrice(99000)
                .releaseDt(LocalDate.of(2026, 6, 1))
                .status(ProductStatus.ACTIVE.name())
                .build());
        productOptionRepository.save(ProductOption.builder()
                .productNo(firstStockProduct.getId())
                .optionName("260")
                .stockCnt(12)
                .additionalPrice(0)
                .build());

        Product secondStockProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("재고 B")
                .modelNum("STOCK-B")
                .releasePrice(99000)
                .releaseDt(LocalDate.of(2026, 6, 1))
                .status(ProductStatus.ACTIVE.name())
                .build());
        productOptionRepository.save(ProductOption.builder()
                .productNo(secondStockProduct.getId())
                .optionName("265")
                .stockCnt(12)
                .additionalPrice(0)
                .build());

        Page<ProductListResDto> releasePriceResult = productRepository.getProductList(
                new ProductListQuery(null, brandEntity.getBrandNo(), null, "릴리즈 프라이스", ProductOrderType.RELEASE_PRICE, false, null, false),
                PageRequest.of(0, 10)
        );
        Page<ProductListResDto> stockCountResult = productRepository.getProductList(
                new ProductListQuery(null, brandEntity.getBrandNo(), null, "재고", ProductOrderType.STOCK_COUNT, false, null, false),
                PageRequest.of(0, 10)
        );

        assertEquals(secondReleasePriceProduct.getId(), releasePriceResult.getContent().getFirst().getProductNo());
        assertEquals(secondStockProduct.getId(), stockCountResult.getContent().getFirst().getProductNo());
        assertTrue(secondReleasePriceProduct.getId() > firstReleasePriceProduct.getId());
        assertTrue(secondStockProduct.getId() > firstStockProduct.getId());
    }

    @Test
    @DisplayName("프론트 카탈로그 조회는 featured, 가격대, 저재고 조건을 함께 반영한다")
    void getFrontCatalogProductsAppliesFeaturedPriceBandAndLowStockFilters() {
        Brand brandEntity = brandRepository.save(Brand.builder()
                .nameKo("뉴발란스")
                .nameEn("New Balance")
                .isActive("Y")
                .build());
        Category categoryEntity = categoryRepository.save(Category.builder()
                .name("러닝화")
                .depth(1)
                .isActive("Y")
                .build());

        Product featuredLowStockProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("990v6 Grey Day")
                .modelNum("M990GL6")
                .releasePrice(189000)
                .releaseDt(LocalDate.of(2026, 6, 1))
                .status(ProductStatus.ACTIVE.name())
                .build());
        productOptionRepository.save(ProductOption.builder()
                .productNo(featuredLowStockProduct.getId())
                .optionName("260")
                .stockCnt(12)
                .additionalPrice(0)
                .build());
        frontProductDisplayRepository.save(FrontProductDisplay.builder()
                .productNo(featuredLowStockProduct.getId())
                .headline("Grey precision")
                .description("대표 노출")
                .mood("Sharp tone")
                .featuredYn("Y")
                .featuredRank(1)
                .build());

        Product stableStockProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("1906U Silver")
                .modelNum("M1906")
                .releasePrice(199000)
                .releaseDt(LocalDate.of(2026, 6, 2))
                .status(ProductStatus.ACTIVE.name())
                .build());
        productOptionRepository.save(ProductOption.builder()
                .productNo(stableStockProduct.getId())
                .optionName("265")
                .stockCnt(44)
                .additionalPrice(0)
                .build());
        frontProductDisplayRepository.save(FrontProductDisplay.builder()
                .productNo(stableStockProduct.getId())
                .headline("Silver lane")
                .description("일반 노출")
                .mood("Calm metal")
                .featuredYn("Y")
                .featuredRank(2)
                .build());

        productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("FuelCell Rebel")
                .modelNum("FC-REBEL")
                .releasePrice(329000)
                .releaseDt(LocalDate.of(2026, 6, 3))
                .status(ProductStatus.ACTIVE.name())
                .build());

        List<FrontCatalogProductRow> result = productRepository.getFrontCatalogProducts(
                new FrontCatalogQuery("Grey", "뉴발란스", "러닝화", "LOW", "FEATURED", 20, true, "UNDER_200")
        );

        assertEquals(1, result.size());
        assertEquals(featuredLowStockProduct.getId(), result.getFirst().productNo());
        assertTrue(result.getFirst().featured());
        assertEquals(12, result.getFirst().totalStock());
    }

    @Test
    @DisplayName("프론트 카탈로그 연관 상품 조회는 같은 브랜드 또는 카테고리의 활성 상품만 featured 우선으로 반환한다")
    void getRelatedFrontCatalogProductsReturnsBrandAndCategoryMatchesFirst() {
        Brand nb = brandRepository.save(Brand.builder().nameKo("뉴발란스").nameEn("New Balance").isActive("Y").build());
        Brand asics = brandRepository.save(Brand.builder().nameKo("아식스").nameEn("ASICS").isActive("Y").build());
        Category running = categoryRepository.save(Category.builder().name("러닝화").depth(1).isActive("Y").build());
        Category trail = categoryRepository.save(Category.builder().name("트레일").depth(1).isActive("Y").build());

        Product target = productRepository.save(Product.builder()
                .brandNo(nb.getBrandNo())
                .categoryNo(running.getCategoryNo())
                .nameKo("990v6 Grey Day")
                .modelNum("M990GL6")
                .releasePrice(289000)
                .releaseDt(LocalDate.of(2026, 6, 1))
                .status(ProductStatus.ACTIVE.name())
                .build());

        Product sameBrand = productRepository.save(Product.builder()
                .brandNo(nb.getBrandNo())
                .categoryNo(trail.getCategoryNo())
                .nameKo("Hierro")
                .modelNum("NB-HIERRO")
                .releasePrice(229000)
                .releaseDt(LocalDate.of(2026, 6, 2))
                .status(ProductStatus.ACTIVE.name())
                .build());
        frontProductDisplayRepository.save(FrontProductDisplay.builder()
                .productNo(sameBrand.getId())
                .headline("Trail grip")
                .description("브랜드 연관")
                .mood("Grip")
                .featuredYn("Y")
                .featuredRank(1)
                .build());
        productOptionRepository.save(ProductOption.builder().productNo(sameBrand.getId()).optionName("270").stockCnt(14).additionalPrice(0).build());

        Product sameCategory = productRepository.save(Product.builder()
                .brandNo(asics.getBrandNo())
                .categoryNo(running.getCategoryNo())
                .nameKo("Gel-Kayano 14")
                .modelNum("1201A019")
                .releasePrice(179000)
                .releaseDt(LocalDate.of(2026, 6, 3))
                .status(ProductStatus.ACTIVE.name())
                .build());
        productOptionRepository.save(ProductOption.builder().productNo(sameCategory.getId()).optionName("265").stockCnt(18).additionalPrice(0).build());

        productRepository.save(Product.builder()
                .brandNo(asics.getBrandNo())
                .categoryNo(trail.getCategoryNo())
                .nameKo("무관 상품")
                .modelNum("OTHER-1")
                .releasePrice(159000)
                .releaseDt(LocalDate.of(2026, 6, 4))
                .status(ProductStatus.ACTIVE.name())
                .build());

        List<FrontCatalogProductRow> related = productRepository.getRelatedFrontCatalogProducts(
                target.getId(),
                nb.getBrandNo(),
                running.getCategoryNo(),
                6
        );

        assertEquals(2, related.size());
        assertEquals(sameBrand.getId(), related.getFirst().productNo());
        assertEquals(sameCategory.getId(), related.get(1).productNo());
    }

    @Test
    @DisplayName("프론트 노출 관리 조회는 설정 여부, featured, 저재고, 키워드를 함께 필터링한다")
    void getAdminFrontDisplayProductsAppliesDisplayFilters() {
        Brand brandEntity = brandRepository.save(Brand.builder().nameKo("뉴발란스").nameEn("New Balance").isActive("Y").build());
        Category categoryEntity = categoryRepository.save(Category.builder().name("러닝화").depth(1).isActive("Y").build());

        Product configuredProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("990v6 Grey Day")
                .modelNum("M990GL6")
                .releasePrice(289000)
                .releaseDt(LocalDate.of(2026, 6, 1))
                .status(ProductStatus.ACTIVE.name())
                .build());
        productOptionRepository.save(ProductOption.builder().productNo(configuredProduct.getId()).optionName("260").stockCnt(12).additionalPrice(0).build());
        frontProductDisplayRepository.save(FrontProductDisplay.builder()
                .productNo(configuredProduct.getId())
                .headline("Grey precision")
                .description("대표 노출")
                .mood("Sharp tone")
                .featuredYn("Y")
                .featuredRank(1)
                .build());

        Product unconfiguredProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("1080v14")
                .modelNum("M1080")
                .releasePrice(219000)
                .releaseDt(LocalDate.of(2026, 6, 2))
                .status(ProductStatus.ACTIVE.name())
                .build());
        productOptionRepository.save(ProductOption.builder().productNo(unconfiguredProduct.getId()).optionName("265").stockCnt(50).additionalPrice(0).build());

        Product incompleteConfiguredProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("2002R Slate")
                .modelNum("M2002RS")
                .releasePrice(199000)
                .releaseDt(LocalDate.of(2026, 6, 3))
                .status(ProductStatus.ACTIVE.name())
                .build());
        productOptionRepository.save(ProductOption.builder().productNo(incompleteConfiguredProduct.getId()).optionName("270").stockCnt(8).additionalPrice(0).build());
        frontProductDisplayRepository.save(FrontProductDisplay.builder()
                .productNo(incompleteConfiguredProduct.getId())
                .headline("Slate rhythm")
                .description(" ")
                .mood("Muted stone")
                .featuredYn("N")
                .featuredRank(999)
                .build());

        List<AdminFrontDisplayProductRow> configuredOnly = productRepository.getAdminFrontDisplayProducts(
                new AdminFrontDisplayProductQuery("Grey", ProductStatus.ACTIVE, brandEntity.getBrandNo(), categoryEntity.getCategoryNo(), true, "READY", true, true, 20, "FEATURED")
        );
        List<AdminFrontDisplayProductRow> unconfiguredOnly = productRepository.getAdminFrontDisplayProducts(
                new AdminFrontDisplayProductQuery(null, ProductStatus.ACTIVE, brandEntity.getBrandNo(), categoryEntity.getCategoryNo(), false, "INCOMPLETE", false, false, 20, "LATEST")
        );
        List<AdminFrontDisplayProductRow> incompleteConfiguredOnly = productRepository.getAdminFrontDisplayProducts(
                new AdminFrontDisplayProductQuery(null, ProductStatus.ACTIVE, brandEntity.getBrandNo(), categoryEntity.getCategoryNo(), true, "INCOMPLETE", false, true, 20, "LATEST")
        );

        assertEquals(1, configuredOnly.size());
        assertEquals(configuredProduct.getId(), configuredOnly.getFirst().productNo());
        assertTrue(configuredOnly.getFirst().featured());
        assertTrue(configuredOnly.getFirst().contentReady());
        assertEquals(1, unconfiguredOnly.size());
        assertEquals(unconfiguredProduct.getId(), unconfiguredOnly.getFirst().productNo());
        assertTrue(!unconfiguredOnly.getFirst().displayConfigured());
        assertEquals(1, incompleteConfiguredOnly.size());
        assertEquals(incompleteConfiguredProduct.getId(), incompleteConfiguredOnly.getFirst().productNo());
        assertTrue(!incompleteConfiguredOnly.getFirst().contentReady());
    }

    @Test
    @DisplayName("featured 순번 충돌 검사는 활성 상품의 전시 메타만 대상으로 한다")
    void existsFeaturedRankConflictChecksOnlyActiveProducts() {
        Brand brandEntity = brandRepository.save(Brand.builder().nameKo("뉴발란스").nameEn("New Balance").isActive("Y").build());
        Category categoryEntity = categoryRepository.save(Category.builder().name("러닝화").depth(1).isActive("Y").build());

        Product activeFeaturedProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("990v6")
                .modelNum("M990GL6")
                .releasePrice(289000)
                .releaseDt(LocalDate.of(2026, 6, 1))
                .status(ProductStatus.ACTIVE.name())
                .build());
        frontProductDisplayRepository.save(FrontProductDisplay.builder()
                .productNo(activeFeaturedProduct.getId())
                .headline("Grey precision")
                .description("대표 노출")
                .mood("Sharp tone")
                .featuredYn("Y")
                .featuredRank(1)
                .build());

        Product hiddenFeaturedProduct = productRepository.save(Product.builder()
                .brandNo(brandEntity.getBrandNo())
                .categoryNo(categoryEntity.getCategoryNo())
                .nameKo("1906U")
                .modelNum("M1906")
                .releasePrice(219000)
                .releaseDt(LocalDate.of(2026, 6, 2))
                .status(ProductStatus.HIDDEN.name())
                .build());
        frontProductDisplayRepository.save(FrontProductDisplay.builder()
                .productNo(hiddenFeaturedProduct.getId())
                .headline("Silver lane")
                .description("숨김 전시")
                .mood("Calm metal")
                .featuredYn("Y")
                .featuredRank(2)
                .build());

        assertTrue(frontProductDisplayRepository.existsFeaturedRankConflict(
                "Y",
                1,
                hiddenFeaturedProduct.getId(),
                ProductStatus.ACTIVE.name()
        ));
        assertTrue(!frontProductDisplayRepository.existsFeaturedRankConflict(
                "Y",
                2,
                activeFeaturedProduct.getId(),
                ProductStatus.ACTIVE.name()
        ));
    }
}
