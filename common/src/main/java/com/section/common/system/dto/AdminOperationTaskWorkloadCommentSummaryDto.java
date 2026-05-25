package com.section.common.system.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminOperationTaskWorkloadCommentSummaryDto {

    private Long assigneeAdminNo;
    private Long taskNo;
    private String taskTitle;
    private Long commentNo;
    private Long adminNo;
    private String adminName;
    private String content;
    private LocalDateTime crtDtm;
}
