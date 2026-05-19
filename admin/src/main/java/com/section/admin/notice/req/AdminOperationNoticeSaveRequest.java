package com.section.admin.notice.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AdminOperationNoticeSaveRequest(
        Long noticeNo,
        @NotBlank(message = "공지 제목은 필수입니다.")
        @Size(max = 200, message = "공지 제목은 200자 이하여야 합니다.")
        String title,
        @NotBlank(message = "공지 내용은 필수입니다.")
        @Size(max = 5000, message = "공지 내용은 5000자 이하여야 합니다.")
        String content,
        String isActive,
        String isPinned,
        LocalDateTime startDtm,
        LocalDateTime endDtm
) {
}
