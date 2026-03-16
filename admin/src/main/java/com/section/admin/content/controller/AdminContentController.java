package com.section.admin.content.controller;

import com.section.admin.content.service.AdminContentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/content")
public class AdminContentController {

    private final AdminContentService adminContentService;

    /**
     * 본인 작성 문서 조회
     * */
    @RequestMapping("/list")
    public ModelAndView contentList(HttpServletRequest req){
        return new ModelAndView("views/index");
    }

}
