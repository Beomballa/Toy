package com.section.common.system.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class AdminOperationTaskListResDto {
    private Long taskNo;
    private String title;
    private String description;
    private String status;
    private String priority;
    private Long assigneeAdminNo;
    private String assigneeAdminName;
    private LocalDate dueDate;
    private String isPinned;
    private LocalDateTime crtDtm;
    private String latestCommentContent;
    private String latestCommentAdminName;
    private LocalDateTime latestCommentDtm;
    private Long commentCount;
}
