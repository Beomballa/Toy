package com.section.admin.product.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.base.entity.type.ProductOrderType;
import com.section.common.base.entity.type.ProductStatus;
import com.section.common.commerce.dto.ProductListQuery;
import com.section.common.commerce.dto.ProductListResDto;
import com.section.common.commerce.dto.ProductStatsDto;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.CategoryRepository;
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
}
