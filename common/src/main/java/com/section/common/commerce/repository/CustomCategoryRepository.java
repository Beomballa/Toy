package com.section.common.commerce.repository;

import com.section.common.commerce.entity.Category;

import java.util.List;

public interface CustomCategoryRepository {
    List<Category> getCategoryList(Integer depth, String keyword, String isActive);
    List<Category> getSubCategoryList(Long parentNo);
}
