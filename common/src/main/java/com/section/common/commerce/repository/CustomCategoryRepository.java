package com.section.common.commerce.repository;

import com.section.common.commerce.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomCategoryRepository {
    Page<Category> getCategoryList(Integer depth, String keyword, String isActive, Pageable pageable);
    List<Category> getSubCategoryList(Long parentNo);
    List<Category> getChildCategories(List<Long> parentNos);
}
