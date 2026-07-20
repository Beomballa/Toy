package com.section.front.system.service;

import com.section.common.base.entity.type.ProductStatus;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
@Transactional
public class FrontLocalCatalogSeeder implements ApplicationRunner {

    static final String DEFAULT_PRODUCT_THUMBNAIL_URL = "/images/product-placeholder.svg";
    private static final int TARGET_ACTIVE_PRODUCT_COUNT = 100;
    private static final int TARGET_FEATURED_COUNT = 12;
    private static final int OPTIONS_PER_PRODUCT = 3;

    private final JdbcTemplate jdbcTemplate;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final FrontProductDisplayRepository frontProductDisplayRepository;

    @Override
    public void run(ApplicationArguments args) {
        ensureFrontDisplayTable();
        seedCatalogBase();
        seedCatalogToTarget();
        seedFrontProductDisplays();
        seedFrontDisplaysToTarget();
    }

    private void ensureFrontDisplayTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS front_product_display (
                    display_no BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    product_no BIGINT NOT NULL,
                    headline VARCHAR(120) NOT NULL,
                    description VARCHAR(1000) NOT NULL,
                    mood VARCHAR(120) NOT NULL,
                    featured_yn VARCHAR(1) NOT NULL DEFAULT 'N',
                    featured_rank INT NOT NULL DEFAULT 999,
                    crt_dtm DATETIME NULL,
                    crt_no BIGINT NULL,
                    upt_dtm DATETIME NULL,
                    upt_no BIGINT NULL,
                    INDEX idx_front_product_display_featured_rank (featured_yn, featured_rank),
                    CONSTRAINT uk_front_product_display_product UNIQUE (product_no)
                )
                """);
    }

    private void seedCatalogBase() {
        if (productRepository.count() > 0) {
            return;
        }

        Brand newBalance = brandRepository.save(Brand.builder().nameKo("New Balance").nameEn("New Balance").logoUrl("/images/brand/newbalance.svg").isActive("Y").build());
        Brand nike = brandRepository.save(Brand.builder().nameKo("Nike").nameEn("Nike").logoUrl("/images/brand/nike.svg").isActive("Y").build());
        Brand asics = brandRepository.save(Brand.builder().nameKo("ASICS").nameEn("ASICS").logoUrl("/images/brand/asics.svg").isActive("Y").build());
        Brand salomon = brandRepository.save(Brand.builder().nameKo("Salomon").nameEn("Salomon").logoUrl("/images/brand/salomon.svg").isActive("Y").build());
        Brand adidas = brandRepository.save(Brand.builder().nameKo("Adidas").nameEn("Adidas").logoUrl("/images/brand/adidas.svg").isActive("Y").build());
        Brand hoka = brandRepository.save(Brand.builder().nameKo("Hoka").nameEn("Hoka").logoUrl("/images/brand/hoka.svg").isActive("Y").build());

        Category sneakers = categoryRepository.save(Category.builder().name("스니커즈").depth(1).isActive("Y").build());
        Category running = categoryRepository.save(Category.builder().parentNo(sneakers.getCategoryNo()).name("러닝화").depth(2).isActive("Y").build());
        Category lifestyle = categoryRepository.save(Category.builder().parentNo(sneakers.getCategoryNo()).name("라이프스타일").depth(2).isActive("Y").build());
        Category outdoor = categoryRepository.save(Category.builder().parentNo(sneakers.getCategoryNo()).name("아웃도어").depth(2).isActive("Y").build());
        Category football = categoryRepository.save(Category.builder().parentNo(sneakers.getCategoryNo()).name("축구화").depth(2).isActive("Y").build());

        seedProduct(newBalance, running, "990v6 Grey Day", "M990GL6", 289000, LocalDate.now().minusDays(2), List.of(option("260", 4), option("270", 6), option("280", 8)));
        seedProduct(nike, lifestyle, "Air Max DN Ember", "DV3337-800", 219000, LocalDate.now().minusDays(3), List.of(option("255", 10), option("265", 24), option("275", 20)));
        seedProduct(asics, running, "Gel-Kayano 14 Oyster", "1201A019-200", 179000, LocalDate.now().minusDays(2), List.of(option("240", 2), option("245", 4), option("250", 6)));
        seedProduct(salomon, outdoor, "XT-6 Skyline", "L47739100", 248000, LocalDate.now().minusDays(4), List.of(option("260", 7), option("270", 12), option("280", 13)));
        seedProduct(adidas, football, "Predator Fold-Over Core", "IG5432", 329000, LocalDate.now().minusDays(5), List.of(option("255", 1), option("265", 3), option("275", 4)));
        seedProduct(hoka, running, "Mach X Voltage", "HM1123", 239000, LocalDate.now().minusDays(8), List.of(option("260", 14), option("270", 25), option("280", 26)));
    }

    private void seedFrontProductDisplays() {
        Map<String, Product> productsByModel = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getModelNum, product -> product, (left, right) -> left));

        saveDisplay(productsByModel.get("M990GL6"), "Grey precision", "브랜드 시그니처 그레이 팔레트에 퍼포먼스 러닝 실루엣을 더한 대표 드롭입니다.", "calm utility", true, 1);
        saveDisplay(productsByModel.get("DV3337-800"), "Ember energy", "강한 주황빛 텐션과 둥근 에어 볼륨이 전면에 드러나는 에너지 중심 모델입니다.", "bold kinetic", true, 2);
        saveDisplay(productsByModel.get("1201A019-200"), "Metal calm", "실버 러닝 무드와 베이지 톤이 섞인 안정적인 실루엣으로 여성 고객 반응이 빠른 편입니다.", "soft metallic", true, 3);
        saveDisplay(productsByModel.get("L47739100"), "Trail machine", "아웃도어 기반 기술 실루엣이지만 도심 착장용 수요가 높은 스테디 라인입니다.", "urban trail", false, 10);
        saveDisplay(productsByModel.get("IG5432"), "Pitch statement", "폴드오버 텅 디테일이 강하고, 콘텐츠용 주목도는 높지만 사이즈별 편차가 큰 모델입니다.", "match focus", false, 11);
        saveDisplay(productsByModel.get("HM1123"), "Fast cushion", "쿠셔닝과 반응성을 동시에 묶은 하이브리드 러닝 카테고리에서 리텐션이 좋은 상품입니다.", "tempo drive", false, 12);
    }

    private void saveDisplay(
            Product product,
            String headline,
            String description,
            String mood,
            boolean featured,
            int featuredRank
    ) {
        if (product == null || frontProductDisplayRepository.findByProductNo(product.getId()).isPresent()) {
            return;
        }
        frontProductDisplayRepository.save(FrontProductDisplay.builder()
                .productNo(product.getId())
                .headline(headline)
                .description(description)
                .mood(mood)
                .featuredYn(featured ? "Y" : "N")
                .featuredRank(featuredRank)
                .build());
    }

    private void seedCatalogToTarget() {
        List<Product> existingProducts = productRepository.findAll();
        int activeCount = (int) existingProducts.stream().filter(Product::isActive).count();
        if (activeCount >= TARGET_ACTIVE_PRODUCT_COUNT) {
            return;
        }

        List<Brand> brands = brandRepository.findByIsActiveOrderByNameKoAsc("Y");
        List<Category> activeCategories = categoryRepository.findByIsActiveOrderByDepthAscNameAscCategoryNoAsc("Y");
        List<Category> categories = activeCategories.stream()
                .filter(category -> category.getDepth() != null && category.getDepth() >= 2)
                .toList();
        if (categories.isEmpty()) {
            categories = activeCategories;
        }
        if (brands.isEmpty() || categories.isEmpty()) {
            log.warn("Skip front demo product seed because active brand or leaf category is missing.");
            return;
        }

        Set<String> existingModels = existingProducts.stream()
                .map(Product::getModelNum)
                .filter(model -> model != null && !model.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
        int sequence = 1;
        while (activeCount < TARGET_ACTIVE_PRODUCT_COUNT) {
            String model = "FRONT-DEMO-%03d".formatted(sequence++);
            if (!existingModels.add(model)) {
                continue;
            }
            int dataIndex = sequence - 2;
            Brand brand = brands.get(dataIndex % brands.size());
            Category category = categories.get(dataIndex % categories.size());
            int price = 89000 + (dataIndex % 16) * 15000;
            LocalDate releaseDate = LocalDate.now().minusDays(dataIndex % 45L);
            seedProduct(
                    brand,
                    category,
                    "%s %s 에디션 %03d".formatted(brand.getNameKo(), category.getName(), dataIndex + 1),
                    model,
                    price,
                    releaseDate,
                    demoOptions(dataIndex)
            );
            activeCount++;
        }
        log.info("Front demo catalog seed completed: {} active products targeted.", TARGET_ACTIVE_PRODUCT_COUNT);
    }

    private List<ProductOption> demoOptions(int index) {
        int baseStock = index % 11;
        return List.of(
                option("250", baseStock, 0),
                option("265", baseStock + 8, index % 4 == 0 ? 5000 : 0),
                option("280", baseStock + 24, index % 5 == 0 ? 10000 : 0)
        );
    }

    private void seedFrontDisplaysToTarget() {
        List<Product> activeProducts = productRepository.findAll().stream()
                .filter(Product::isActive)
                .limit(TARGET_ACTIVE_PRODUCT_COUNT)
                .toList();
        Map<Long, FrontProductDisplay> displaysByProduct = frontProductDisplayRepository.findAll().stream()
                .collect(Collectors.toMap(FrontProductDisplay::getProductNo, display -> display, (left, right) -> left));
        Set<Integer> featuredRanks = displaysByProduct.values().stream()
                .filter(FrontProductDisplay::isFeatured)
                .map(FrontProductDisplay::getFeaturedRank)
                .collect(Collectors.toCollection(HashSet::new));

        int featuredCount = featuredRanks.size();
        int nextRank = 1;
        int displayIndex = 0;
        for (Product product : activeProducts) {
            if (displaysByProduct.containsKey(product.getId())) {
                continue;
            }
            while (featuredRanks.contains(nextRank)) {
                nextRank++;
            }
            boolean featured = featuredCount < TARGET_FEATURED_COUNT;
            int featuredRank = featured ? nextRank : 999;
            if (featured) {
                featuredRanks.add(nextRank++);
                featuredCount++;
            }
            frontProductDisplayRepository.save(FrontProductDisplay.builder()
                    .productNo(product.getId())
                    .headline("오늘의 셀렉션 %03d".formatted(++displayIndex))
                    .description("브랜드, 카테고리, 가격대와 재고 흐름을 함께 확인할 수 있도록 구성한 프론트 데모 상품입니다.")
                    .mood(displayIndex % 3 == 0 ? "daily essential" : displayIndex % 3 == 1 ? "new classic" : "street utility")
                    .featuredYn(featured ? "Y" : "N")
                    .featuredRank(featuredRank)
                    .build());
        }
        log.info("Front display seed completed: up to {} active product displays targeted.", TARGET_ACTIVE_PRODUCT_COUNT);
    }

    private void seedProduct(
            Brand brand,
            Category category,
            String name,
            String model,
            int price,
            LocalDate releaseDate,
            List<ProductOption> options
    ) {
        Product product = productRepository.save(Product.builder()
                .brandNo(brand.getBrandNo())
                .categoryNo(category.getCategoryNo())
                .nameKo(name)
                .modelNum(model)
                .releasePrice(price)
                .releaseDt(releaseDate)
                .thumbnailUrl(DEFAULT_PRODUCT_THUMBNAIL_URL)
                .status(ProductStatus.ACTIVE.name())
                .build());

        productOptionRepository.saveAll(options.stream()
                .map(option -> ProductOption.builder()
                        .productNo(product.getId())
                        .optionName(option.getOptionName())
                        .stockCnt(option.getStockCnt())
                        .additionalPrice(option.getAdditionalPrice())
                        .build())
                .toList());
    }

    private ProductOption option(String optionName, int stockCnt) {
        return option(optionName, stockCnt, 0);
    }

    private ProductOption option(String optionName, int stockCnt, int additionalPrice) {
        return ProductOption.builder()
                .optionName(optionName)
                .stockCnt(stockCnt)
                .additionalPrice(additionalPrice)
                .build();
    }
}
