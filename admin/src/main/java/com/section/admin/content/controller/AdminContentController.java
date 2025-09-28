package com.section.admin.content.controller;

import com.section.admin.content.req.ContentListReqDto;
import com.section.admin.content.res.ContentGetResDto;
import com.section.admin.content.res.ContentMyDocResDto;
import com.section.admin.content.service.AdminContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/content")
public class AdminContentController {

    private final AdminContentService adminContentService;

    /**
     * 등록한 데이터
     * */
    @RequestMapping("/list")
    public ModelAndView contentList(ContentListReqDto reqDto){
        ModelAndView mav = new ModelAndView("views/content-list");
        ContentMyDocResDto posts = adminContentService.listDocument();
//        mav.addObject("reqDto",reqDto);
        mav.addObject("posts",posts);
        return mav;
    }

    /**
     * 상세 조회
     * */
    @RequestMapping("/edit")
    public ModelAndView contentEdit(@RequestParam(name = "docNo", required = false) String docNo){
        ModelAndView mav = new ModelAndView("views/content-edit");
        if(docNo != null && !docNo.isEmpty()){
            ContentGetResDto result = adminContentService.getDocumentInfo(docNo);
            mav.addObject("docNo",docNo);
            mav.addObject("result", result);
        }
        return mav;
    }
}
