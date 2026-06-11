package com.section.admin.user.req;

import com.section.common.base.entity.type.YN;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AccountListQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminMemberListRequest {

    private String keyword;
    private String masterYn;
    private String delYn;
    private String initYn;

    public AccountListQuery toQuery() {
        return new AccountListQuery(
                normalize(keyword),
                parseYn(masterYn),
                parseYn(delYn),
                parseYn(initYn)
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
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
