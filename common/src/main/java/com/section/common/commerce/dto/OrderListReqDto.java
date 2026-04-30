package com.section.common.commerce.dto;

import com.section.common.base.entity.type.OrderStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Setter
public class OrderListReqDto {
    private static final long MAX_DATE_RANGE_DAYS = 92L;

    private String status;
    private String searchKeyword;
    private String startDate;
    private String endDate;

    public OrderListQuery toQuery() {
        LocalDateTime startDateTime = parseStartDateTime();
        LocalDateTime endDateTime = parseEndDateTime();

        // 기간 조건은 조회 결과 왜곡을 막기 위해 요청 경계에서 먼저 정합성을 확인합니다.
        if (startDateTime != null && endDateTime != null && startDateTime.isAfter(endDateTime)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        // 주문 목록은 운영자가 자주 반복 조회하는 화면이라, 과도한 기간 조회는 요청 경계에서 먼저 제한합니다.
        if (startDateTime != null && endDateTime != null
                && startDateTime.toLocalDate().plusDays(MAX_DATE_RANGE_DAYS).isBefore(endDateTime.toLocalDate())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return new OrderListQuery(
                parseStatus(),
                normalizeKeyword(),
                startDateTime,
                endDateTime
        );
    }

    private OrderStatus parseStatus() {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizeKeyword() {
        if (searchKeyword == null || searchKeyword.isBlank()) {
            return null;
        }
        return searchKeyword.trim();
    }

    private LocalDateTime parseStartDateTime() {
        if (startDate == null || startDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(startDate).atStartOfDay();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private LocalDateTime parseEndDateTime() {
        if (endDate == null || endDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(endDate).atTime(LocalTime.MAX);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
