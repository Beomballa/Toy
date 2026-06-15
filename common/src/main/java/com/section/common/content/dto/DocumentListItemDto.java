package com.section.common.content.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DocumentListItemDto {
    private Long id;
    private String boardType;
    private String status;
    private String publicYn;
    private String pinnedYn;
    private String title;
    private String contentPreview;
    private int viewCnt;
    private Long productNo;
    private LocalDateTime crtDtm;
}
