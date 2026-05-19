package com.section.common.system.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminOperationNoticeListResDto {

    private Long noticeNo;
    private String title;
    private String content;
    private String isActive;
    private String isPinned;
    private LocalDateTime startDtm;
    private LocalDateTime endDtm;
    private LocalDateTime crtDtm;
}
