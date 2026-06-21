package com.section.admin.notice.res;

import com.section.admin.log.res.AdminLogDetailResponse;
import com.section.admin.log.res.AdminLogListResponse;

import java.util.List;

public record AdminOperationNoticeHistoryListResponse(
        List<Item> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize,
        long rangeStart,
        long rangeEnd,
        String pageInfoLabel,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static AdminOperationNoticeHistoryListResponse from(AdminLogListResponse response, String returnTo) {
        return new AdminOperationNoticeHistoryListResponse(
                response.items().stream().map(item -> Item.from(item, returnTo)).toList(),
                response.totalElements(),
                response.totalPages(),
                response.currentPage(),
                response.pageSize(),
                response.rangeStart(),
                response.rangeEnd(),
                response.pageInfoLabel(),
                AppliedQuery.from(response.appliedQuery(), returnTo),
                ResultMeta.from(response.resultMeta())
        );
    }

    public record Item(
            Long logNo,
            Long noticeNo,
            String noticeLabel,
            String noticePath,
            String actionType,
            String actionLabel,
            Long adminNo,
            String adminName,
            String ipAddress,
            String actionDtm,
            String logDetailPath
    ) {
        private static Item from(AdminLogListResponse.Item item, String returnTo) {
            Long noticeNo = item.targetId();
            String noticePath = noticeNo == null ? null : "/admin/settings/notices?noticeNo=" + noticeNo + "&returnTo=" + encode(returnTo);
            return new Item(
                    item.logNo(),
                    noticeNo,
                    item.targetLabel(),
                    noticePath,
                    item.actionType(),
                    resolveActionLabel(item.actionType()),
                    item.adminNo(),
                    item.adminName(),
                    item.ipAddress(),
                    item.actionDtm(),
                    "/api/admin/logs/get?no=" + item.logNo()
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

        private static String encode(String value) {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    public record AppliedQuery(
            Long noticeNo,
            String actionType,
            Long adminNo,
            String adminKeyword,
            String startDate,
            String endDate,
            String returnTo
    ) {
        public static AppliedQuery from(AdminLogListResponse.AppliedQuery query, String returnTo) {
            return new AppliedQuery(
                    query.targetId(),
                    query.actionType(),
                    query.adminNo(),
                    query.adminKeyword(),
                    query.startDate(),
                    query.endDate(),
                    returnTo
            );
        }
    }

    public record ResultMeta(
            String resultLabel,
            String pageInfoLabel,
            int filterCount,
            String querySignature
    ) {
        public static ResultMeta from(AdminLogListResponse.ResultMeta meta) {
            return new ResultMeta(meta.resultLabel(), meta.pageInfoLabel(), meta.filterCount(), meta.querySignature());
        }
    }
}
