package com.section.admin.content.req;

import com.section.common.base.entity.type.YN;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.entity.Document;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class ContentBulkOperateRequest {
    private List<Long> ids;
    private String status;
    private String publicYn;
    private String pinnedYn;

    public Set<Long> normalizedIds() {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null || id <= 0) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            normalized.add(id);
        }
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    public Document.PublishStatus normalizedStatus() {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return Document.PublishStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public YN normalizedPublicYn() {
        return parseYn(publicYn);
    }

    public YN normalizedPinnedYn() {
        return parseYn(pinnedYn);
    }

    public boolean hasOperateField() {
        return normalizedStatus() != null || normalizedPublicYn() != null || normalizedPinnedYn() != null;
    }

    private YN parseYn(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return YN.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
