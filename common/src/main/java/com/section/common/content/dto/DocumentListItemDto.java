package com.section.common.content.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentListItemDto {
    private String docNo;
    private String title;
    private String content;
    private String uptDtm;
    private String viewYn;
}
