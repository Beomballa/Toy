package com.section.admin.task.res;

import com.section.admin.log.res.AdminLogListResponse;

import java.util.List;

public record AdminOperationTaskHistoryListResponse(
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
    public static AdminOperationTaskHistoryListResponse from(AdminLogListResponse response, String returnTo) {
        return new AdminOperationTaskHistoryListResponse(
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
            Long taskNo,
            String taskLabel,
            String taskPath,
            String actionType,
            String actionLabel,
            Long adminNo,
            String adminName,
            String ipAddress,
            String actionDtm,
            String logDetailPath
    ) {
        private static Item from(AdminLogListResponse.Item item, String returnTo) {
            Long taskNo = item.targetId();
            String taskPath = taskNo == null ? null : "/admin/settings/tasks/get?no=" + taskNo + "&returnTo=" + encode(returnTo);
            return new Item(
                    item.logNo(),
                    taskNo,
                    item.targetLabel(),
                    taskPath,
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
                case "TASK_CREATE" -> "작업 생성";
                case "TASK_UPDATE" -> "작업 수정";
                case "TASK_STATUS_UPDATE" -> "상태 변경";
                case "TASK_DUPLICATE" -> "작업 복제";
                case "TASK_BULK_UPDATE" -> "일괄 변경";
                case "TASK_BULK_DUPLICATE" -> "일괄 복제";
                case "TASK_COMMENT_CREATE" -> "댓글 등록";
                case "TASK_COMMENT_UPDATE" -> "댓글 수정";
                case "TASK_COMMENT_DELETE" -> "댓글 삭제";
                case "TASK_BULK_DELETE" -> "일괄 삭제";
                case "TASK_DELETE" -> "작업 삭제";
                default -> actionType == null ? "-" : actionType;
            };
        }

        private static String encode(String value) {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    public record AppliedQuery(
            Long taskNo,
            String actionType,
            Long adminNo,
            String startDate,
            String endDate,
            String returnTo
    ) {
        public static AppliedQuery from(AdminLogListResponse.AppliedQuery query, String returnTo) {
            return new AppliedQuery(
                    query.targetId(),
                    query.actionType(),
                    query.adminNo(),
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
