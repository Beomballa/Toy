package com.section.admin.task.support;

import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.system.dto.AdminOperationTaskListQuery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record AdminOperationTaskExportSummary(
        String exportedAt,
        String sortLabel,
        String filterSummary
) {
    private static final DateTimeFormatter EXPORTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static AdminOperationTaskExportSummary from(
            AdminOperationTaskListQuery query,
            Map<Long, String> assigneeNameMap
    ) {
        List<String> filters = new ArrayList<>();
        if (query.keyword() != null) {
            filters.add("검색어: " + query.keyword());
        }
        if (query.status() != null) {
            filters.add("상태: " + AdminOperationTaskStatus.fromCode(query.status()).getLabel());
        }
        if (query.priority() != null) {
            filters.add("우선순위: " + AdminOperationTaskPriority.fromCode(query.priority()).getLabel());
        }
        if (query.assigneeAdminNo() != null) {
            filters.add("담당자: " + assigneeNameMap.getOrDefault(query.assigneeAdminNo(), "관리자#" + query.assigneeAdminNo()));
        }
        if ("Y".equalsIgnoreCase(query.isPinned())) {
            filters.add("고정만");
        } else if ("N".equalsIgnoreCase(query.isPinned())) {
            filters.add("일반만");
        }
        if ("Y".equalsIgnoreCase(query.overdueOnly())) {
            filters.add("기한초과만");
        }
        if ("Y".equalsIgnoreCase(query.unassignedOnly())) {
            filters.add("미지정만");
        }
        if ("Y".equalsIgnoreCase(query.commentedOnly())) {
            filters.add("메모있는 작업만");
        }
        if (query.dueDateFrom() != null || query.dueDateTo() != null) {
            filters.add("기한: "
                    + (query.dueDateFrom() == null ? "시작없음" : query.dueDateFrom())
                    + " ~ "
                    + (query.dueDateTo() == null ? "종료없음" : query.dueDateTo()));
        }

        return new AdminOperationTaskExportSummary(
                LocalDateTime.now().format(EXPORTED_AT_FORMAT),
                resolveSortLabel(query.sortBy()),
                filters.isEmpty() ? "전체" : String.join(" | ", filters)
        );
    }

    private static String resolveSortLabel(String sortBy) {
        if (sortBy == null || sortBy.isBlank() || "PINNED_DUE".equalsIgnoreCase(sortBy)) {
            return "고정 우선 · 마감 임박 순";
        }
        return switch (sortBy.toUpperCase()) {
            case "DUE_DATE_DESC" -> "마감일 늦은 순";
            case "PRIORITY_DESC" -> "우선순위 높은 순";
            case "CREATED_DESC" -> "최근 등록 순";
            default -> "고정 우선 · 마감 임박 순";
        };
    }
}
