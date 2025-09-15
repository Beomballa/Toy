package com.section.admin.content.req;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentSetReqDto {

    private String title;
    private String content;

    private String contentTypeCode;
    private String status;
    private String reserveYn;
    private String reserveDtm;
    private String viewYn;
}
