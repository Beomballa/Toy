package com.section.admin.category.res;

import com.section.common.commerce.entity.Category;

public record CategoryResponse(
        Long categoryNo,
        Long parentNo,
        String name,
        Integer depth,
        String isActive
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getCategoryNo(),
                category.getParentNo(),
                category.getName(),
                category.getDepth(),
                category.getIsActive()
        );
    }
}
