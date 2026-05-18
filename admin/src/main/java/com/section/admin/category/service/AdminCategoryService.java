package com.section.admin.category.service;

import com.section.admin.category.req.CategoryListRequest;
import com.section.admin.category.req.CategorySaveRequest;
import com.section.admin.category.res.CategoryListResponse;
import com.section.admin.category.res.CategoryResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.commerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryListResponse getCategoryListByDepth(CategoryListRequest req) {
        Page<Category> categoryPage = categoryRepository.getCategoryList(
                        req.getDepth(),
                        req.normalizedKeyword(),
                        req.normalizedIsActive(),
                        PageRequest.of(req.normalizedPage(), req.normalizedSize())
                );
        Page<CategoryResponse> responsePage = categoryPage.map(CategoryResponse::from);
        return CategoryListResponse.of(responsePage, req);
    }

    public List<CategoryResponse> getSubCategories(Long parentNo) {
        return categoryRepository.getSubCategoryList(parentNo).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public Category getCategoryEntity(Long categoryNo) {
        return categoryRepository.findById(categoryNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    @Transactional
    public void saveCategory(CategorySaveRequest req) {
        if (req.categoryNo() != null) {
            Category category = getCategoryEntity(req.categoryNo());
            category.update(req.name(), req.isActive() != null ? req.isActive() : "Y");
        } else {
            categoryRepository.save(Category.builder()
                    .parentNo(req.parentNo())
                    .name(req.name())
                    .depth(req.depth())
                    .isActive(req.isActive() != null ? req.isActive() : "Y")
                    .build());
        }
    }

    @Transactional
    public void deleteCategory(Long categoryNo) {
        if (productRepository.existsByCategoryNo(categoryNo)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        categoryRepository.deleteById(categoryNo);
    }

    @Transactional
    public void updateActive(Long categoryNo, String isActive) {
        String normalized = isActive == null ? null : isActive.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Category category = getCategoryEntity(categoryNo);
        category.changeStatus(normalized);
    }
}
