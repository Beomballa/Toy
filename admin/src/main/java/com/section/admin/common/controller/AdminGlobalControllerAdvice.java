package com.section.admin.common.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AdminGlobalControllerAdvice {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + queryString;
    }

    @ModelAttribute("currentBoardType")
    public String currentBoardType(HttpServletRequest request) {
        return request.getParameter("boardType");
    }
}
