package com.section.admin.content.service;

import com.section.admin.content.req.ContentSetReqDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminContentService {


    public void setContent(ContentSetReqDto reqDto) {

    }
}
