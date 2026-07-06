package com.section.admin.content.req;

import com.section.common.base.entity.type.YN;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.entity.Document;

import java.util.Locale;

public record ContentQuickOperateRequest(
        String status,
        String publicYn,
        String pinnedYn
) {
    public boolean hasOperateField() {
        return normalizedStatus() != null || normalizedPublicYn() != null || normalizedPinnedYn() != null;
    }

    public Document.PublishStatus normalizedStatus() {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return Document.PublishStatus.valueOf(normalizeEnumValue(status));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public YN normalizedPublicYn() {
        return parseYn(publicYn);
    }

    public YN normalizedPinnedYn() {
        return parseYn(pinnedYn);
    }

    private YN parseYn(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return YN.valueOf(normalizeEnumValue(value));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizeEnumValue(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
