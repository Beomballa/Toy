package com.section.front.system.controller;

import com.section.front.system.dto.FrontApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice(basePackages = "com.section.front.controller")
public class FrontGlobalExceptionHandler {

    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<FrontApiErrorResponse> handleInvalidRequest(Exception exception) {
        log.debug("Invalid front API request: {}", exception.getMessage());
        return response(HttpStatus.BAD_REQUEST, "F001", "요청 조건이 올바르지 않습니다.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<FrontApiErrorResponse> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == HttpStatus.NOT_FOUND) {
            return response(HttpStatus.NOT_FOUND, "F002", "상품을 찾을 수 없습니다.");
        }
        log.warn("Unexpected front response status: {}", exception.getStatusCode());
        return response(HttpStatus.BAD_REQUEST, "F001", "요청을 처리할 수 없습니다.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<FrontApiErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected front API error", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "F003", "상품 정보를 불러오지 못했습니다.");
    }

    private ResponseEntity<FrontApiErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new FrontApiErrorResponse(code, message, status.value()));
    }
}
