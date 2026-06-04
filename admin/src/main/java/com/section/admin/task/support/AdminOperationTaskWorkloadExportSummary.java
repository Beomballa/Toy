package com.section.admin.task.support;

import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.system.dto.AdminOperationTaskWorkloadListQuery;

import java.time.LocalDate;

public record AdminOperationTaskWorkloadExportSummary(
        String exportedDate,
        String filterSummary
) {
    public static AdminOperationTaskWorkloadExportSummary of(AdminOperationTaskWorkloadListQuery query, LocalDate exportedDate) {
        return new AdminOperationTaskWorkloadExportSummary(
                exportedDate.toString(),
                buildFilterSummary(query)
        );
    }

    private static String buildFilterSummary(AdminOperationTaskWorkloadListQuery query) {
        StringBuilder builder = new StringBuilder("기한 초과 우선 · 진행중 우선");
        if (query.keyword() != null) {
            builder.append(" · 검색=").append(query.keyword());
        }
        if (query.priority() != null) {
            builder.append(" · 우선순위=").append(AdminOperationTaskPriority.fromCode(query.priority()).getLabel());
        }
        if ("Y".equalsIgnoreCase(query.overdueOnly())) {
            builder.append(" · 기한 초과만");
        }
        return builder.toString();
    }
}
