package com.section.admin.task.res;

import com.section.admin.log.res.AdminLogSourceLinkSupport;
import com.section.common.base.entity.type.AdminOperationTaskPriority;
import com.section.common.base.entity.type.AdminOperationTaskStatus;
import com.section.common.system.dto.AdminOperationTaskListQuery;
import com.section.common.system.dto.AdminOperationTaskListResDto;
import com.section.common.system.dto.AdminOperationTaskSummaryDto;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOperationTaskListResponse(
        List<Item> items,
        int currentPage,
        int totalPages,
        long totalElements,
        int pageSize,
        TaskStats taskStats,
        List<AssigneeOption> assigneeOptions,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static AdminOperationTaskListResponse of(
            Page<AdminOperationTaskListResDto> page,
            AdminOperationTaskListQuery query,
            AdminOperationTaskSummaryDto summary,
            List<AssigneeOption> assigneeOptions,
            LocalDate today
    ) {
        return new AdminOperationTaskListResponse(
                page.getContent().stream().map(item -> Item.from(item, today)).toList(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                TaskStats.from(summary, query.toStatsQuery()),
                assigneeOptions,
                new AppliedQuery(query.keyword(), query.status(), query.priority(), query.assigneeAdminNo(), query.overdueOnly()),
                ResultMeta.from(page, query)
        );
    }

    public record Item(
            Long taskNo,
            String title,
            String description,
            String status,
            String statusLabel,
            String priority,
            String priorityLabel,
            Long assigneeAdminNo,
            String assigneeAdminName,
            String dueDate,
            String dueState,
            String isPinned,
            String crtDtm,
            String historyPath,
            String historyLabel,
            String activityLogPath,
            String activityLogLabel
    ) {
        static Item from(AdminOperationTaskListResDto item, LocalDate today) {
            return new Item(
                    item.getTaskNo(),
                    item.getTitle(),
                    item.getDescription(),
                    item.getStatus(),
                    AdminOperationTaskStatus.fromCode(item.getStatus()).getLabel(),
                    item.getPriority(),
                    AdminOperationTaskPriority.fromCode(item.getPriority()).getLabel(),
                    item.getAssigneeAdminNo(),
                    item.getAssigneeAdminName() == null ? "-" : item.getAssigneeAdminName(),
                    item.getDueDate() == null ? "-" : item.getDueDate().toString(),
                    resolveDueState(item, today),
                    item.getIsPinned(),
                    format(item.getCrtDtm()),
                    "/admin/settings/tasks/history?taskNo=" + item.getTaskNo(),
                    "이력",
                    AdminLogSourceLinkSupport.resolveTaskLogPath(item.getTaskNo()),
                    "활동 로그"
            );
        }

        private static String resolveDueState(AdminOperationTaskListResDto item, LocalDate today) {
            if (item.getDueDate() == null) {
                return "기한 없음";
            }
            if ("DONE".equalsIgnoreCase(item.getStatus())) {
                return "완료";
            }
            if (item.getDueDate().isBefore(today)) {
                return "기한 초과";
            }
            if (item.getDueDate().isEqual(today)) {
                return "오늘 마감";
            }
            return "진행중";
        }

        private static String format(LocalDateTime value) {
            return value == null ? "-" : value.toString().replace('T', ' ');
        }
    }

    public record TaskStats(
            long totalCount,
            long todoCount,
            long inProgressCount,
            long overdueCount,
            String contextLabel,
            String querySignature
    ) {
        static TaskStats from(AdminOperationTaskSummaryDto summary, AdminOperationTaskListQuery query) {
            return new TaskStats(
                    summary.totalCount(),
                    summary.todoCount(),
                    summary.inProgressCount(),
                    summary.overdueCount(),
                    buildContextLabel(query),
                    buildQuerySignature(query)
            );
        }

        private static String buildContextLabel(AdminOperationTaskListQuery query) {
            return query.keyword() == null && query.assigneeAdminNo() == null ? "기본 문맥 기준" : "탐색 문맥 기준";
        }

        private static String buildQuerySignature(AdminOperationTaskListQuery query) {
            StringBuilder builder = new StringBuilder("고정 우선 · 마감 임박 순");
            if (query.keyword() != null) {
                builder.append(" · 검색=").append(query.keyword());
            }
            if (query.assigneeAdminNo() != null) {
                builder.append(" · 담당자=").append(query.assigneeAdminNo());
            }
            return builder.toString();
        }
    }

    public record AssigneeOption(
            Long adminNo,
            String name
    ) {
    }

    public record AppliedQuery(
            String keyword,
            String status,
            String priority,
            Long assigneeAdminNo,
            String overdueOnly
    ) {
    }

    public record ResultMeta(
            String resultLabel,
            String pageInfoLabel,
            long appliedFilterCount,
            boolean hasActiveFilters,
            String querySignature,
            long rangeStart,
            long rangeEnd
    ) {
        static ResultMeta from(Page<AdminOperationTaskListResDto> page, AdminOperationTaskListQuery query) {
            long totalElements = page.getTotalElements();
            long rangeStart = totalElements == 0 ? 0 : (long) page.getNumber() * page.getSize() + 1;
            long rangeEnd = totalElements == 0 ? 0 : rangeStart + page.getNumberOfElements() - 1;
            long filterCount = countFilters(query);
            boolean hasActiveFilters = filterCount > 0;

            return new ResultMeta(
                    hasActiveFilters ? "검색 결과 %d건".formatted(totalElements) : "전체 %d건".formatted(totalElements),
                    totalElements == 0
                            ? "조건에 맞는 운영 작업이 없습니다."
                            : "%d-%d / %d건 · %d페이지".formatted(rangeStart, rangeEnd, totalElements, Math.max(page.getTotalPages(), 1)),
                    filterCount,
                    hasActiveFilters,
                    buildQuerySignature(query),
                    rangeStart,
                    rangeEnd
            );
        }

        private static long countFilters(AdminOperationTaskListQuery query) {
            long count = 0;
            if (query.keyword() != null) count++;
            if (query.status() != null) count++;
            if (query.priority() != null) count++;
            if (query.assigneeAdminNo() != null) count++;
            if (query.overdueOnly() != null) count++;
            return count;
        }

        private static String buildQuerySignature(AdminOperationTaskListQuery query) {
            StringBuilder builder = new StringBuilder("고정 우선 · 마감 임박 순");
            if (query.keyword() != null) builder.append(" · 검색=").append(query.keyword());
            if (query.status() != null) builder.append(" · 상태=").append(AdminOperationTaskStatus.fromCode(query.status()).getLabel());
            if (query.priority() != null) builder.append(" · 우선순위=").append(AdminOperationTaskPriority.fromCode(query.priority()).getLabel());
            if (query.assigneeAdminNo() != null) builder.append(" · 담당자=").append(query.assigneeAdminNo());
            if ("Y".equalsIgnoreCase(query.overdueOnly())) builder.append(" · 기한초과만");
            return builder.toString();
        }
    }
}
