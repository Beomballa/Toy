package com.section.common.system.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminOperationTaskCommentResDto {

    private Long commentNo;
    private Long taskNo;
    private Long adminNo;
    private String adminName;
    private String content;
    private LocalDateTime crtDtm;
}
