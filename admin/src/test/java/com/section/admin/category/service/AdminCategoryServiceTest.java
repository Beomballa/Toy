package com.section.admin.category.service;

import com.section.admin.category.req.CategoryBulkDeleteRequest;
import com.section.admin.category.req.CategoryBulkOperateRequest;
import com.section.admin.category.req.CategoryListRequest;
import com.section.admin.category.req.CategorySaveRequest;
import com.section.admin.category.res.CategoryListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.repository.CategoryRepository;
import com.section.common.commerce.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
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

        when(categoryRepository.getCategoryList(1, "신발", "Y", PageRequest.of(0, 10))).thenReturn(new PageImpl<>(
                List.of(Category.builder().categoryNo(1L).name("신발").depth(1).isActive("Y").build()),
                PageRequest.of(0, 10),
                1
        ));

        CategoryListResponse response = adminCategoryService.getCategoryListByDepth(request);

        assertEquals(1, response.items().size());
        assertEquals(0, response.currentPage());
        assertEquals(10, response.pageSize());
        assertEquals(1L, response.totalElements());
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

    @Test
    @DisplayName("카테고리 일괄 상태 변경은 변경 건수와 동일 상태 건수를 구분한다")
    void bulkOperateCountsUpdatedAndUnchanged() {
        Category category1 = Category.builder().categoryNo(1L).name("신발").depth(1).isActive("Y").build();
        Category category2 = Category.builder().categoryNo(2L).name("러닝화").depth(2).isActive("N").build();
        when(categoryRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(category1, category2));

        AdminCategoryService.BulkOperateResult result = adminCategoryService.bulkOperate(
                new CategoryBulkOperateRequest(List.of(1L, 2L), "N")
        );

        assertEquals(2, result.requestedCount());
        assertEquals(1, result.updatedCount());
        assertEquals(1, result.unchangedCount());
        assertEquals("N", category1.getIsActive());
        assertEquals("N", category2.getIsActive());
    }

    @Test
    @DisplayName("카테고리 저장은 같은 부모 아래 중복명을 허용하지 않는다")
    void saveCategoryRejectsDuplicateNameWithinSameParent() {
        when(categoryRepository.existsByParentNoAndDepthAndNameIgnoreCase(null, 1, "신발")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                adminCategoryService.saveCategory(new CategorySaveRequest(null, null, " 신발 ", 1, "Y")));

        assertEquals(ErrorCode.CATEGORY_NAME_DUPLICATED, exception.getErrorCode());
    }

    @Test
    @DisplayName("2뎁스 카테고리는 1뎁스 부모가 필수다")
    void saveCategoryRejectsMissingParentForDepthTwo() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                adminCategoryService.saveCategory(new CategorySaveRequest(null, null, "러닝화", 2, "Y")));

        assertEquals(ErrorCode.CATEGORY_HIERARCHY_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("하위 카테고리가 있으면 상위 카테고리를 삭제할 수 없다")
    void deleteCategoryRejectsWhenHasChildren() {
        when(categoryRepository.existsByParentNo(1L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> adminCategoryService.deleteCategory(1L));

        assertEquals(ErrorCode.CATEGORY_HAS_CHILDREN, exception.getErrorCode());
    }

    @Test
    @DisplayName("카테고리 저장은 공백 정규화 후 저장한다")
    void saveCategoryNormalizesName() {
        adminCategoryService.saveCategory(new CategorySaveRequest(null, null, " 러닝   화 ", 1, "y"));

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("카테고리 저장은 공백뿐인 이름을 거부한다")
    void saveCategoryRejectsBlankName() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                adminCategoryService.saveCategory(new CategorySaveRequest(null, null, "   ", 1, "Y")));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("카테고리 수정은 부모와 depth 변경을 반영한다")
    void saveCategoryUpdatesHierarchyOnEdit() {
        Category parent = Category.builder().categoryNo(10L).name("신발").depth(1).isActive("Y").build();
        Category category = Category.builder().categoryNo(2L).name("기존").depth(1).isActive("Y").build();
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        adminCategoryService.saveCategory(new CategorySaveRequest(2L, 10L, " 러닝 화 ", 2, "n"));

        assertEquals(10L, category.getParentNo());
        assertEquals(2, category.getDepth());
        assertEquals("러닝 화", category.getName());
        assertEquals("N", category.getIsActive());
    }

    @Test
    @DisplayName("카테고리 CSV 내보내기는 현재 필터와 루트 목록을 기록한다")
    void exportCategoryListCsvIncludesSummaryAndRows() {
        CategoryListRequest request = new CategoryListRequest();
        request.setKeyword("신발");
        request.setIsActive("Y");
        request.setDepth(1);

        when(categoryRepository.getCategoryList(1, "신발", "Y", PageRequest.of(0, 1000))).thenReturn(new PageImpl<>(
                List.of(Category.builder().categoryNo(11L).parentNo(null).name("신발").depth(1).isActive("Y").build()),
                PageRequest.of(0, 1000),
                1
        ));

        String csv = new String(adminCategoryService.exportCategoryListCsv(request), UTF_8);

        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("대분류 기준 · 검색=신발 · 상태=사용"));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("\"11\",\"-\",\"신발\",\"1\",\"사용중\""));
    }

    @Test
    @DisplayName("카테고리 삭제는 존재하는 카테고리 엔티티를 삭제한다")
    void deleteCategoryDeletesExistingEntity() {
        Category category = Category.builder().categoryNo(5L).name("액세서리").depth(1).isActive("Y").build();
        when(categoryRepository.existsByParentNo(5L)).thenReturn(false);
        when(productRepository.existsByCategoryNo(5L)).thenReturn(false);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        adminCategoryService.deleteCategory(5L);

        verify(categoryRepository).delete(argThat(item -> item.getCategoryNo().equals(5L)));
    }

    @Test
    @DisplayName("카테고리 일괄 삭제는 하위 카테고리나 상품 연관이 있는 대상을 건너뛴다")
    void bulkDeleteSkipsBlockedCategories() {
        Category category1 = Category.builder().categoryNo(5L).name("액세서리").depth(1).isActive("Y").build();
        Category category2 = Category.builder().categoryNo(6L).name("러닝화").depth(2).isActive("Y").build();
        when(categoryRepository.findAllById(List.of(5L, 6L, 9L))).thenReturn(List.of(category1, category2));
        when(categoryRepository.existsByParentNo(5L)).thenReturn(false);
        when(productRepository.existsByCategoryNo(5L)).thenReturn(false);
        when(categoryRepository.existsByParentNo(6L)).thenReturn(false);
        when(productRepository.existsByCategoryNo(6L)).thenReturn(true);

        AdminCategoryService.BulkDeleteResult result = adminCategoryService.bulkDelete(
                new CategoryBulkDeleteRequest(List.of(5L, 6L, 9L))
        );

        assertEquals(3, result.requestedCount());
        assertEquals(1, result.deletedCount());
        assertEquals(1, result.blockedCount());
        assertEquals(1, result.missingCount());
        verify(categoryRepository).delete(argThat(item -> item.getCategoryNo().equals(5L)));
    }
}
