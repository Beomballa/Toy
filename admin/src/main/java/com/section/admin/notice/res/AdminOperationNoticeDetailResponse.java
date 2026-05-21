package com.section.admin.notice.res;

import com.section.admin.log.res.AdminLogListResponse;
import com.section.common.system.entity.AdminOperationNotice;

import java.time.LocalDateTime;
import java.util.List;

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
        String activityLogPath,
        List<RecentHistory> recentHistories
) {
    public static AdminOperationNoticeDetailResponse from(AdminOperationNotice notice, List<AdminLogListResponse.Item> recentHistories) {
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
                "/admin/settings/logs?actionType=NOTICE_&targetId=" + notice.getNoticeNo(),
                recentHistories == null ? List.of() : recentHistories.stream().map(RecentHistory::from).toList()
        );
    }

    public record RecentHistory(
            Long logNo,
            String actionType,
            String actionLabel,
            String adminName,
            String actionDtm,
            String activityLogPath,
            String historyPath
    ) {
        public static RecentHistory from(AdminLogListResponse.Item item) {
            return new RecentHistory(
                item.logNo(),
                item.actionType(),
                resolveActionLabel(item.actionType()),
                item.adminName(),
                item.actionDtm(),
                "/admin/settings/logs?actionType=" + item.actionType() + "&targetId=" + item.targetId(),
                item.targetId() == null ? null : "/admin/settings/notices/history?noticeNo=" + item.targetId()
            );
        }

        private static String resolveActionLabel(String actionType) {
            return switch (actionType) {
                case "NOTICE_CREATE" -> "공지 생성";
                case "NOTICE_UPDATE" -> "공지 수정";
                case "NOTICE_ACTIVE_UPDATE" -> "공지 상태 변경";
                case "NOTICE_DELETE" -> "공지 삭제";
                case "NOTICE_BULK_UPDATE" -> "공지 일괄 변경";
                default -> actionType == null ? "-" : actionType;
            };
        }
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
