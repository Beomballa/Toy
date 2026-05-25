package com.section.admin.task.res;

import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.system.dto.AdminOperationTaskWorkloadCommentSummaryDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadDto;
import com.section.common.system.dto.AdminOperationTaskWorkloadListQuery;
import com.section.common.system.dto.AdminOperationTaskWorkloadSummaryDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public record AdminOperationTaskWorkloadListResponse(
        List<Item> items,
        int currentPage,
        int totalPages,
        long totalElements,
        int pageSize,
        Summary summary,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static AdminOperationTaskWorkloadListResponse of(
            Page<AdminOperationTaskWorkloadDto> page,
            AdminOperationTaskWorkloadListQuery query,
            AdminOperationTaskWorkloadSummaryDto summary,
            Map<Long, AdminOperationTaskWorkloadCommentSummaryDto> latestCommentMap
    ) {
        return new AdminOperationTaskWorkloadListResponse(
                page.getContent().stream().map(item -> Item.from(item, latestCommentMap == null ? null : latestCommentMap.get(item.assigneeAdminNo()))).toList(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                Summary.from(summary, query),
                new AppliedQuery(query.keyword(), query.priority(), query.overdueOnly()),
                ResultMeta.from(page, query)
        );
    }

    public record Item(
            Long assigneeAdminNo,
            String assigneeAdminName,
            long totalCount,
            long todoCount,
            long inProgressCount,
            long overdueCount,
            String latestCommentTaskTitle,
            String latestCommentContent,
            String latestCommentAdminName,
            String latestCommentDtm,
            String targetPath,
            String overduePath
    ) {
        static Item from(AdminOperationTaskWorkloadDto item, AdminOperationTaskWorkloadCommentSummaryDto latestComment) {
            String assigneeName = item.assigneeAdminName() == null || item.assigneeAdminName().isBlank()
                    ? "미지정"
                    : item.assigneeAdminName();
            String targetPath = item.assigneeAdminNo() == null
                    ? "/admin/settings/tasks?unassignedOnly=Y"
                    : "/admin/settings/tasks?assigneeAdminNo=" + item.assigneeAdminNo();
            String overduePath = item.assigneeAdminNo() == null
                    ? "/admin/settings/tasks?unassignedOnly=Y&overdueOnly=Y"
                    : "/admin/settings/tasks?assigneeAdminNo=" + item.assigneeAdminNo() + "&overdueOnly=Y";
            return new Item(
                    item.assigneeAdminNo(),
                    assigneeName,
                    item.totalCount(),
                    item.todoCount(),
                    item.inProgressCount(),
                    item.overdueCount(),
                    latestComment == null ? null : latestComment.getTaskTitle(),
                    latestComment == null ? null : latestComment.getContent(),
                    latestComment == null ? null : latestCommentAdminName(latestComment),
                    latestComment == null ? null : formatDateTime(latestComment.getCrtDtm()),
                    targetPath,
                    overduePath
            );
        }

        private static String latestCommentAdminName(AdminOperationTaskWorkloadCommentSummaryDto latestComment) {
            if (latestComment.getAdminName() != null && !latestComment.getAdminName().isBlank()) {
                return latestComment.getAdminName();
            }
            return latestComment.getAdminNo() == null ? "관리자" : "관리자#" + latestComment.getAdminNo();
        }

        private static String formatDateTime(java.time.LocalDateTime value) {
            return value == null ? "-" : value.toString().replace('T', ' ');
        }
    }

    public record Summary(
            long assigneeCount,
            long assignedTaskCount,
            long overdueTaskCount,
            long unassignedTaskCount,
            String contextLabel,
            String querySignature
    ) {
        static Summary from(AdminOperationTaskWorkloadSummaryDto summary, AdminOperationTaskWorkloadListQuery query) {
            return new Summary(
                    summary.assigneeCount(),
                    summary.assignedTaskCount(),
                    summary.overdueTaskCount(),
                    summary.unassignedTaskCount(),
                    query.keyword() == null && query.priority() == null && query.overdueOnly() == null ? "전체 운영 작업 기준" : "탐색 문맥 기준",
                    buildQuerySignature(query)
            );
        }
    }

    public record AppliedQuery(
            String keyword,
            String priority,
            String overdueOnly
    ) {
    }

    public record ResultMeta(
            String resultLabel,
            String pageInfoLabel,
            long filterCount,
            boolean hasActiveFilters,
            String querySignature,
            long rangeStart,
            long rangeEnd
    ) {
        static ResultMeta from(Page<AdminOperationTaskWorkloadDto> page, AdminOperationTaskWorkloadListQuery query) {
            long totalElements = page.getTotalElements();
            long rangeStart = totalElements == 0 ? 0 : (long) page.getNumber() * page.getSize() + 1;
            long rangeEnd = totalElements == 0 ? 0 : rangeStart + page.getNumberOfElements() - 1;
            long filterCount = countFilters(query);
            boolean hasActiveFilters = filterCount > 0;
            return new ResultMeta(
                    hasActiveFilters ? "검색 결과 %d명".formatted(totalElements) : "전체 %d명".formatted(totalElements),
                    totalElements == 0
                            ? "조건에 맞는 담당자 워크로드가 없습니다."
                            : "%d-%d / %d명 · %d페이지".formatted(rangeStart, rangeEnd, totalElements, Math.max(page.getTotalPages(), 1)),
                    filterCount,
                    hasActiveFilters,
                    buildQuerySignature(query),
                    rangeStart,
                    rangeEnd
            );
        }
    }

    private static long countFilters(AdminOperationTaskWorkloadListQuery query) {
        long count = 0;
        if (query.keyword() != null) count++;
        if (query.priority() != null) count++;
        if (query.overdueOnly() != null) count++;
        return count;
    }

    private static String buildQuerySignature(AdminOperationTaskWorkloadListQuery query) {
        StringBuilder builder = new StringBuilder("기한 초과 우선 · 진행중 우선");
        if (query.keyword() != null) {
            builder.append(" · 검색=").append(query.keyword());
        }
        if (query.priority() != null) {
            builder.append(" · 우선순위=").append(AdminOperationTaskPriority.fromCode(query.priority()).getLabel());
        }
        if ("Y".equalsIgnoreCase(query.overdueOnly())) {
            builder.append(" · 기한초과 작업만");
        }
        return builder.toString();
    }
}
