package com.section.admin.log.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.system.dto.AdminActivityLogListQuery;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AdminLogListRequest {

    private Long adminNo;
    private String actionType;
    private Long targetId;
    private LocalDate startDate;
    private LocalDate endDate;

    public AdminActivityLogListQuery toQuery() {
        if (adminNo != null && adminNo <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (targetId != null && targetId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new AdminActivityLogListQuery(
                adminNo,
                normalize(actionType),
                targetId,
                startDate,
                endDate
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }
}
