package com.section.admin.category.service;

import com.section.admin.category.req.CategoryListRequest;
import com.section.admin.category.res.CategoryListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.repository.CategoryRepository;
import com.section.common.commerce.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AdminCategoryService adminCategoryService;

    @Test
    @DisplayName("카테고리 목록은 키워드와 상태로 필터링된다")
    void getCategoryListFiltersByKeywordAndActive() {
        CategoryListRequest request = new CategoryListRequest();
        request.setKeyword("신발");
        request.setIsActive("Y");

        when(categoryRepository.getCategoryList(1, "신발", "Y")).thenReturn(List.of(
                Category.builder().categoryNo(1L).name("신발").depth(1).isActive("Y").build()
        ));

        CategoryListResponse response = adminCategoryService.getCategoryListByDepth(request);

        assertEquals(1, response.items().size());
        assertEquals("검색 결과 1건", response.resultMeta().resultLabel());
        assertEquals("대분류 기준 · 검색=신발 · 상태=사용", response.resultMeta().querySignature());
    }

    @Test
    @DisplayName("카테고리 삭제는 연결 상품이 있으면 거부한다")
    void deleteCategoryRejectsWhenProductsExist() {
        when(productRepository.existsByCategoryNo(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> adminCategoryService.deleteCategory(1L));
    }

    @Test
    @DisplayName("카테고리 활성 상태 변경은 YN 값만 허용한다")
    void updateActiveChangesStatus() {
        Category category = Category.builder().categoryNo(1L).name("신발").depth(1).isActive("Y").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        adminCategoryService.updateActive(1L, "N");

        assertEquals("N", category.getIsActive());
    }
}
