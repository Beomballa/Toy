package com.section.front.system.service;

import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.entity.FrontProductDisplay;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.CategoryRepository;
import com.section.common.commerce.repository.FrontProductDisplayRepository;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FrontLocalCatalogSeederTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductOptionRepository productOptionRepository;
    @Mock
    private FrontProductDisplayRepository frontProductDisplayRepository;

    @InjectMocks
    private FrontLocalCatalogSeeder seeder;

    @Test
    void topsUpActiveProductsAndDisplaysToOneHundred() throws Exception {
        Brand brand = Brand.builder().brandNo(1L).nameKo("테스트 브랜드").nameEn("Test Brand").isActive("Y").build();
        Category category = Category.builder().categoryNo(1L).name("스니커즈").depth(2).isActive("Y").build();
        List<Product> products = new ArrayList<>();
        List<FrontProductDisplay> displays = new ArrayList<>();
        for (long index = 1; index <= 98; index++) {
            products.add(product(index, "EXISTING-%03d".formatted(index), brand, category));
            displays.add(FrontProductDisplay.builder()
                    .displayNo(index)
                    .productNo(index)
                    .headline("기존 노출 " + index)
                    .description("기존 설명")
                    .mood("existing")
                    .featuredYn("N")
                    .featuredRank(999)
                    .build());
        }

        AtomicLong productSequence = new AtomicLong(98);
        when(productRepository.count()).thenReturn(98L);
        when(productRepository.findAll()).thenAnswer(invocation -> new ArrayList<>(products));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product source = invocation.getArgument(0);
            Product saved = Product.builder()
                    .id(productSequence.incrementAndGet())
                    .brandNo(source.getBrandNo())
                    .categoryNo(source.getCategoryNo())
                    .nameKo(source.getNameKo())
                    .modelNum(source.getModelNum())
                    .releasePrice(source.getReleasePrice())
                    .releaseDt(source.getReleaseDt())
                    .thumbnailUrl(source.getThumbnailUrl())
                    .status(source.getStatus())
                    .build();
            products.add(saved);
            return saved;
        });
        when(brandRepository.findByIsActiveOrderByNameKoAsc("Y")).thenReturn(List.of(brand));
        when(categoryRepository.findByIsActiveOrderByDepthAscNameAscCategoryNoAsc("Y")).thenReturn(List.of(category));
        when(frontProductDisplayRepository.findAll()).thenAnswer(invocation -> new ArrayList<>(displays));
        when(frontProductDisplayRepository.save(any(FrontProductDisplay.class))).thenAnswer(invocation -> {
            FrontProductDisplay display = invocation.getArgument(0);
            displays.add(display);
            return display;
        });

        seeder.run(null);

        assertThat(products).hasSize(100);
        assertThat(displays).hasSize(100);
        assertThat(products.subList(98, 100))
                .allSatisfy(product -> assertThat(product.getThumbnailUrl())
                        .isEqualTo("/images/product/category/sneakers.jpg"));
        verify(productRepository, times(2)).save(any(Product.class));
        verify(productOptionRepository, times(2)).saveAll(any());
        verify(frontProductDisplayRepository, times(2)).save(any(FrontProductDisplay.class));
    }

    @Test
    void repairsOnlyMissingAndLegacyGeneratedThumbnailPaths() {
        Product missingThumbnail = product(1L, "MISSING-001", null);
        Product legacyThumbnail = product(2L, "LEGACY-002", "/images/product/legacy-002.png");
        Product placeholderThumbnail = product(3L, "PLACEHOLDER-003", FrontLocalCatalogSeeder.DEFAULT_PRODUCT_THUMBNAIL_URL);
        Product customThumbnail = product(4L, "CUSTOM-004", "https://cdn.example.com/custom-004.png");

        assertThat(seeder.repairThumbnailIfNecessary(missingThumbnail, "스니커즈")).isTrue();
        assertThat(seeder.repairThumbnailIfNecessary(legacyThumbnail, "스니커즈")).isTrue();
        assertThat(seeder.repairThumbnailIfNecessary(placeholderThumbnail, "러닝화")).isTrue();
        assertThat(seeder.repairThumbnailIfNecessary(customThumbnail, "러닝화")).isFalse();
        assertThat(missingThumbnail.getThumbnailUrl()).isEqualTo("/images/product/category/sneakers.jpg");
        assertThat(legacyThumbnail.getThumbnailUrl()).isEqualTo("/images/product/category/sneakers.jpg");
        assertThat(placeholderThumbnail.getThumbnailUrl()).isEqualTo("/images/product/category/running.jpg");
        assertThat(customThumbnail.getThumbnailUrl()).isEqualTo("https://cdn.example.com/custom-004.png");
    }

    @Test
    void fallsBackToDefaultThumbnailForUnknownCategory() {
        assertThat(FrontLocalCatalogSeeder.thumbnailForCategory("미등록 카테고리"))
                .isEqualTo(FrontLocalCatalogSeeder.DEFAULT_PRODUCT_THUMBNAIL_URL);
    }

    private Product product(long id, String model, Brand brand, Category category) {
        return product(id, model, null, brand, category);
    }

    private Product product(long id, String model, String thumbnailUrl) {
        Brand brand = Brand.builder().brandNo(1L).nameKo("테스트 브랜드").build();
        Category category = Category.builder().categoryNo(1L).name("스니커즈").build();
        return product(id, model, thumbnailUrl, brand, category);
    }

    private Product product(long id, String model, String thumbnailUrl, Brand brand, Category category) {
        return Product.builder()
                .id(id)
                .brandNo(brand.getBrandNo())
                .categoryNo(category.getCategoryNo())
                .nameKo("기존 상품 " + id)
                .modelNum(model)
                .releasePrice(150000)
                .thumbnailUrl(thumbnailUrl)
                .status(ProductStatus.ACTIVE.name())
                .build();
    }
}
