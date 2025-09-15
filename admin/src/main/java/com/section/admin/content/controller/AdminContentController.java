package com.section.admin.content.controller;

import com.section.admin.content.req.ContentListReqDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequestMapping("/content/normal")
public class AdminContentController {

    @RequestMapping("/list")
    public ModelAndView list(ContentListReqDto reqDto){
        ModelAndView mav = new ModelAndView("views/normal-list");
        mav.addObject("reqDto",reqDto);
        return mav;
    }
}
