package com.section.admin.product.req;

public record CategorySaveRequest(
        Long categoryNo,
        Long parentNo,
        String name,
        Integer depth,
        String isActive
) {}
