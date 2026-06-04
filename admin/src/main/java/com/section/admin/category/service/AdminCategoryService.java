package com.section.admin.category.service;

import com.section.admin.category.req.CategoryBulkDeleteRequest;
import com.section.admin.category.req.CategoryBulkOperateRequest;
import com.section.admin.category.req.CategoryListRequest;
import com.section.admin.category.req.CategorySaveRequest;
import com.section.admin.category.res.CategoryListResponse;
import com.section.admin.category.res.CategoryResponse;
import com.section.admin.category.support.CategoryExportCsvWriter;
import com.section.admin.category.support.CategoryExportSummary;
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

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCategoryService {
    private static final int CATEGORY_EXPORT_MAX_SIZE = 1000;

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

    public byte[] exportCategoryListCsv(CategoryListRequest req) {
        Page<Category> categoryPage = categoryRepository.getCategoryList(
                req.getDepth(),
                req.normalizedKeyword(),
                req.normalizedIsActive(),
                PageRequest.of(0, CATEGORY_EXPORT_MAX_SIZE)
        );
        return CategoryExportCsvWriter.write(
                CategoryExportSummary.of(req, java.time.LocalDateTime.now()),
                categoryPage.getContent().stream().map(CategoryResponse::from).toList()
        );
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
        Category category = getCategoryEntity(categoryNo);
        categoryRepository.delete(category);
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

    @Transactional
    public BulkOperateResult bulkOperate(CategoryBulkOperateRequest req) {
        req.validateOperation();
        List<Long> targetCategoryNos = req.normalizedCategoryNos();
        String normalizedIsActive = req.normalizedIsActive();

        List<Category> categories = categoryRepository.findAllById(targetCategoryNos);
        if (categories.isEmpty()) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        int updatedCount = 0;
        int unchangedCount = 0;
        for (Category category : categories) {
            if (normalizedIsActive.equals(category.getIsActive())) {
                unchangedCount += 1;
                continue;
            }
            category.changeStatus(normalizedIsActive);
            updatedCount += 1;
        }
        return new BulkOperateResult(targetCategoryNos.size(), updatedCount, unchangedCount);
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
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    private String normalizeYnStatus(String value) {
        String normalized = value == null ? "Y" : value.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    @Transactional
    public BulkDeleteResult bulkDelete(CategoryBulkDeleteRequest req) {
        List<Long> targetCategoryNos = req.normalizedCategoryNos();
        List<Category> categories = categoryRepository.findAllById(targetCategoryNos);
        if (categories.isEmpty()) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        int deletedCount = 0;
        int blockedCount = 0;
        for (Category category : categories) {
            if (categoryRepository.existsByParentNo(category.getCategoryNo())
                    || productRepository.existsByCategoryNo(category.getCategoryNo())) {
                blockedCount += 1;
                continue;
            }
            categoryRepository.delete(category);
            deletedCount += 1;
        }

        HashSet<Long> existingCategoryNoSet = new HashSet<>(categories.stream()
                .map(Category::getCategoryNo)
                .toList());
        long missingCount = targetCategoryNos.stream()
                .filter(no -> !existingCategoryNoSet.contains(no))
                .count();
        return new BulkDeleteResult(targetCategoryNos.size(), deletedCount, blockedCount, (int) missingCount);
    }

    public record BulkOperateResult(
            int requestedCount,
            int updatedCount,
            int unchangedCount
    ) {
    }

    public record BulkDeleteResult(
            int requestedCount,
            int deletedCount,
            int blockedCount,
            int missingCount
    ) {
    }
}
