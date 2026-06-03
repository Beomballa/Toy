package com.section.admin.settings.res;

import com.section.admin.settings.support.AdminSettingDefinition;
import com.section.common.system.dto.AdminSystemSettingHistoryListQuery;
import com.section.common.system.dto.AdminSystemSettingHistoryListResDto;
import com.section.common.system.dto.AdminSystemSettingHistorySummaryDto;
import org.springframework.data.domain.Page;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public record AdminSystemSettingHistoryListResponse(
        List<Item> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize,
        long rangeStart,
        long rangeEnd,
        String pageInfoLabel,
        Summary summary,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AdminSystemSettingHistoryListResponse from(
            Page<AdminSystemSettingHistoryListResDto> page,
            Map<Long, String> adminNameMap,
            AdminSystemSettingHistoryListQuery query,
            AdminSystemSettingHistorySummaryDto summary
    ) {
        long totalElements = page.getTotalElements();
        long rangeStart = totalElements == 0 ? 0 : (long) page.getNumber() * page.getSize() + 1;
        long rangeEnd = totalElements == 0 ? 0 : rangeStart + page.getNumberOfElements() - 1;
        return new AdminSystemSettingHistoryListResponse(
                page.getContent().stream().map(item -> Item.from(item, adminNameMap)).toList(),
                totalElements,
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                rangeStart,
                rangeEnd,
                totalElements == 0
                        ? "조건에 맞는 설정 변경 이력이 없습니다."
                        : "%d-%d / %d건 · %d페이지".formatted(rangeStart, rangeEnd, totalElements, Math.max(page.getTotalPages(), 1)),
                Summary.from(summary),
                AppliedQuery.from(query),
                ResultMeta.from(page, query, rangeStart, rangeEnd)
        );
    }

    public record Item(
            Long historyNo,
            String settingKey,
            String settingName,
            String beforeValue,
            String afterValue,
            String beforeValueLabel,
            String afterValueLabel,
            String changeSummary,
            Long changedAdminNo,
            String changedAdminName,
            String changedIpAddress,
            String changedAt
    ) {
        public static Item from(AdminSystemSettingHistoryListResDto item, Map<Long, String> adminNameMap) {
            AdminSettingDefinition definition = AdminSettingDefinition.fromKey(item.getSettingKey());
            Long changedAdminNo = item.getCrtNo();
            return new Item(
                    item.getHistoryNo(),
                    item.getSettingKey(),
                    item.getSettingName(),
                    item.getBeforeValue(),
                    item.getAfterValue(),
                    definition.formatValue(item.getBeforeValue()),
                    definition.formatValue(item.getAfterValue()),
                    item.getChangeSummary(),
                    changedAdminNo,
                    changedAdminNo == null ? "관리자" : adminNameMap.getOrDefault(changedAdminNo, "관리자#" + changedAdminNo),
                    item.getChangedIpAddress(),
                    item.getCrtDtm() == null ? "-" : item.getCrtDtm().format(DATE_TIME_FORMATTER)
            );
        }
    }

    public record Summary(
            long totalCount,
            long todayCount,
            long maintenanceCount,
            long communityCount,
            long orderExportCount,
            long lowStockThresholdCount
    ) {
        private static Summary from(AdminSystemSettingHistorySummaryDto summary) {
            if (summary == null) {
                return new Summary(0, 0, 0, 0, 0, 0);
            }
            return new Summary(
                    summary.totalCount(),
                    summary.todayCount(),
                    summary.maintenanceCount(),
                    summary.communityCount(),
                    summary.orderExportCount(),
                    summary.lowStockThresholdCount()
            );
        }
    }

    public record AppliedQuery(
            String settingKey,
            Long adminNo,
            String startDate,
            String endDate
    ) {
        private static AppliedQuery from(AdminSystemSettingHistoryListQuery query) {
            return new AppliedQuery(
                    query.settingKey(),
                    query.adminNo(),
                    query.startDate() == null ? null : query.startDate().toString(),
                    query.endDate() == null ? null : query.endDate().toString()
            );
        }
    }

    public record ResultMeta(
            String resultLabel,
            String pageInfoLabel,
            int filterCount,
            String querySignature
    ) {
        private static ResultMeta from(
                Page<AdminSystemSettingHistoryListResDto> page,
                AdminSystemSettingHistoryListQuery query,
                long rangeStart,
                long rangeEnd
        ) {
            int filterCount = countFilters(query);
            String resultLabel = page.getTotalElements() == 0
                    ? "조회 결과 없음"
                    : "%d-%d / %d건".formatted(rangeStart, rangeEnd, page.getTotalElements());
            String querySignature = buildQuerySignature(query);
            return new ResultMeta(
                    resultLabel,
                    page.getTotalElements() == 0
                            ? "조건에 맞는 설정 변경 이력이 없습니다."
                            : "%d-%d / %d건 · %d페이지".formatted(rangeStart, rangeEnd, page.getTotalElements(), Math.max(page.getTotalPages(), 1)),
                    filterCount,
                    querySignature
            );
        }

        private static int countFilters(AdminSystemSettingHistoryListQuery query) {
            int count = 0;
            if (query.settingKey() != null) {
                count++;
            }
            if (query.adminNo() != null) {
                count++;
            }
            if (query.startDate() != null) {
                count++;
            }
            if (query.endDate() != null) {
                count++;
            }
            return count;
        }

        private static String buildQuerySignature(AdminSystemSettingHistoryListQuery query) {
            StringBuilder builder = new StringBuilder("최신 변경순");
            if (query.settingKey() != null) {
                builder.append(" · 설정=").append(AdminSettingDefinition.fromKey(query.settingKey()).label());
            }
            if (query.adminNo() != null) {
                builder.append(" · 관리자=#").append(query.adminNo());
            }
            if (query.startDate() != null || query.endDate() != null) {
                builder.append(" · 기간=")
                        .append(query.startDate() == null ? "-" : query.startDate())
                        .append("~")
                        .append(query.endDate() == null ? "-" : query.endDate());
            }
            return builder.toString();
        }
    }
}
