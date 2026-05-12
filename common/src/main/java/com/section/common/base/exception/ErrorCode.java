package com.section.common.base.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 메서드입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "엔티티를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "잘못된 타입입니다."),

    // Product & Commerce
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "존재하지 않는 상품입니다."),
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "존재하지 않는 브랜드입니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CA001", "존재하지 않는 카테고리입니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "존재하지 않는 주문입니다."),
    ORDER_STATUS_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "O002", "현재 주문 상태에서는 요청한 작업을 수행할 수 없습니다."),

    // Member
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "존재하지 않는 회원입니다."),
    EMAIL_DUPLICATION(HttpStatus.BAD_REQUEST, "M002", "이미 가입된 이메일입니다."),

    // Content
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "존재하지 않는 게시물입니다."),

    // Admin operation policy
    ADMIN_MAINTENANCE_MODE(HttpStatus.SERVICE_UNAVAILABLE, "A001", "현재 관리자 유지보수 모드입니다."),
    ADMIN_FEATURE_DISABLED(HttpStatus.BAD_REQUEST, "A002", "현재 설정으로 비활성화된 기능입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
