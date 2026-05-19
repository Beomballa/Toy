package com.section.admin.notice.res;

import com.section.common.system.entity.AdminOperationNotice;

import java.time.LocalDateTime;

public record AdminOperationNoticeDetailResponse(
        Long noticeNo,
        String title,
        String content,
        String isActive,
        String isPinned,
        String startDtm,
        String endDtm
) {
    public static AdminOperationNoticeDetailResponse from(AdminOperationNotice notice) {
        return new AdminOperationNoticeDetailResponse(
                notice.getNoticeNo(),
                notice.getTitle(),
                notice.getContent(),
                notice.getIsActive(),
                notice.getIsPinned(),
                format(notice.getStartDtm()),
                format(notice.getEndDtm())
        );
    }

    private static String format(LocalDateTime value) {
        return value == null ? "-" : value.toString().replace('T', ' ');
    }
}
