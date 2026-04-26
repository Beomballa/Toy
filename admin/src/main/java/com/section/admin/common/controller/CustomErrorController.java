package com.section.admin.common.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        // 500 에러(서버 로직 오류)인 경우엔 리다이렉트 하지 말고 에러 페이지를 보여줘서 개발자가 알 수 있게 함
        if (status != null && Integer.parseInt(status.toString()) == 500) {
            return "error";
        }

        // 그 외(404 등)는 전부 메인으로
        return "redirect:/product/list";
    }
}