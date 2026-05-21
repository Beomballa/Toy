package com.section.admin.notice.res;

import com.section.common.system.entity.AdminOperationNotice;

import java.time.LocalDateTime;

public record AdminOperationNoticeDetailResponse(
        Long noticeNo,
        String title,
        String content,
        String isActive,
        String isPinned,
        String displayStatus,
        String startDtm,
        String endDtm,
        String crtDtm,
        String historyPath,
        String activityLogPath
) {
    public static AdminOperationNoticeDetailResponse from(AdminOperationNotice notice) {
        return new AdminOperationNoticeDetailResponse(
                notice.getNoticeNo(),
                notice.getTitle(),
                notice.getContent(),
                notice.getIsActive(),
                notice.getIsPinned(),
                resolveDisplayStatus(notice),
                format(notice.getStartDtm()),
                format(notice.getEndDtm()),
                format(notice.getCrtDtm()),
                "/admin/settings/notices/history?noticeNo=" + notice.getNoticeNo(),
                "/admin/settings/logs?actionType=NOTICE_&targetId=" + notice.getNoticeNo()
        );
    }

    private static String resolveDisplayStatus(AdminOperationNotice notice) {
        if (!"Y".equalsIgnoreCase(notice.getIsActive())) {
            return "비활성";
        }
        LocalDateTime now = LocalDateTime.now();
        if (notice.getStartDtm() != null && notice.getStartDtm().isAfter(now)) {
            return "예약";
        }
        if (notice.getEndDtm() != null && notice.getEndDtm().isBefore(now)) {
            return "종료";
        }
        return "노출중";
    }

    private static String format(LocalDateTime value) {
        return value == null ? "-" : value.toString().replace('T', ' ');
    }
}
