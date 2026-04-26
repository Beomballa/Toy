package com.section.admin.category.req;

public record CategorySaveRequest(
        Long categoryNo,
        Long parentNo,
        String name,
        Integer depth,
        String isActive
) {}
