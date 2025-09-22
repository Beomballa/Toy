package com.section.admin.content.controller;

import com.section.admin.content.req.ContentListReqDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
//@RequestMapping("/main")
public class DashBoardController {
    @RequestMapping("/main")
    public ModelAndView dashBoardList(ContentListReqDto reqDto){
        ModelAndView mav = new ModelAndView("views/dashboard");
        mav.addObject("reqDto",reqDto);
        return mav;
    }
}
