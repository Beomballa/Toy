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
        String normalizedName = normalizeRequiredText(req.name());
        String normalizedIsActive = normalizeYnStatus(req.isActive());

        validateCategoryHierarchy(req.categoryNo(), req.parentNo(), req.depth());
        validateDuplicateCategoryName(req.categoryNo(), req.parentNo(), req.depth(), normalizedName);

        if (req.categoryNo() != null) {
            Category category = getCategoryEntity(req.categoryNo());
            category.update(req.parentNo(), normalizedName, req.depth(), normalizedIsActive);
        } else {
            categoryRepository.save(Category.builder()
                    .parentNo(req.parentNo())
                    .name(normalizedName)
                    .depth(req.depth())
                    .isActive(normalizedIsActive)
                    .build());
        }
    }

    @Transactional
    public void deleteCategory(Long categoryNo) {
        if (categoryRepository.existsByParentNo(categoryNo)) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }
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

    private void validateCategoryHierarchy(Long categoryNo, Long parentNo, Integer depth) {
        if (depth == null || depth < 1 || depth > 2) {
            throw new BusinessException(ErrorCode.CATEGORY_HIERARCHY_INVALID);
        }

        if (depth == 1) {
            if (parentNo != null) {
                throw new BusinessException(ErrorCode.CATEGORY_HIERARCHY_INVALID);
            }
            return;
        }

        if (parentNo == null) {
            throw new BusinessException(ErrorCode.CATEGORY_HIERARCHY_INVALID);
        }

        if (categoryNo != null && categoryRepository.existsByParentNo(categoryNo) && depth != 1) {
            throw new BusinessException(ErrorCode.CATEGORY_HIERARCHY_INVALID);
        }

        Category parent = getCategoryEntity(parentNo);
        if (parent.getDepth() != 1 || parent.getParentNo() != null) {
            throw new BusinessException(ErrorCode.CATEGORY_HIERARCHY_INVALID);
        }
        if (categoryNo != null && categoryNo.equals(parentNo)) {
            throw new BusinessException(ErrorCode.CATEGORY_HIERARCHY_INVALID);
        }
    }

    private void validateDuplicateCategoryName(Long categoryNo, Long parentNo, Integer depth, String name) {
        boolean duplicated = categoryNo == null
                ? categoryRepository.existsByParentNoAndDepthAndNameIgnoreCase(parentNo, depth, name)
                : categoryRepository.existsByParentNoAndDepthAndNameIgnoreCaseAndCategoryNoNot(parentNo, depth, name, categoryNo);
        if (duplicated) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_DUPLICATED);
        }
    }

    private String normalizeRequiredText(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeYnStatus(String value) {
        String normalized = value == null ? "Y" : value.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }
}
