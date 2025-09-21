package com.section.admin.content.controller;

import com.section.admin.content.req.ContentListReqDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequestMapping("/content")
public class AdminContentController {

    @RequestMapping("/list")
    public ModelAndView contentList(ContentListReqDto reqDto){
        ModelAndView mav = new ModelAndView("views/content-list");
        mav.addObject("reqDto",reqDto);
        return mav;
    }

    @RequestMapping("/edit")
    public ModelAndView editList(ContentListReqDto reqDto){
        ModelAndView mav = new ModelAndView("views/content-set");
        mav.addObject("reqDto",reqDto);
        return mav;
    }
}
