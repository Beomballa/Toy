package com.section.common.system.dto;

import com.section.common.base.entity.type.YN;

public record AccountListQuery(
        String keyword,
        YN masterYn,
        YN delYn
) {
}
