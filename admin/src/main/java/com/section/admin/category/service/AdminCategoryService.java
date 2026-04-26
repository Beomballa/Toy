package com.section.admin.category.service;

import com.section.admin.category.req.CategorySaveRequest;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getCategoryListByDepth(Integer depth) {
        return categoryRepository.findByDepth(depth);
    }

    public List<Category> getSubCategories(Long parentNo) {
        return categoryRepository.findByParentNo(parentNo);
    }

    public Category getCategory(Long categoryNo) {
        return categoryRepository.findById(categoryNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    @Transactional
    public void saveCategory(CategorySaveRequest req) {
        if (req.categoryNo() != null) {
            Category category = getCategory(req.categoryNo());
            category.update(req.name(), req.isActive());
        } else {
            categoryRepository.save(Category.builder()
                    .parentNo(req.parentNo())
                    .name(req.name())
                    .depth(req.depth())
                    .isActive(req.isActive())
                    .build());
        }
    }

    @Transactional
    public void deleteCategory(Long categoryNo) {
        categoryRepository.deleteById(categoryNo);
    }
}
