package com.section.admin.settings.res;

import com.section.admin.settings.support.AdminSettingDefinition;
import com.section.common.system.dto.AdminSystemSettingHistoryListResDto;
import org.springframework.data.domain.Page;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public record AdminSystemSettingHistoryListResponse(
        List<Item> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AdminSystemSettingHistoryListResponse from(
            Page<AdminSystemSettingHistoryListResDto> page,
            Map<Long, String> adminNameMap
    ) {
        return new AdminSystemSettingHistoryListResponse(
                page.getContent().stream().map(item -> Item.from(item, adminNameMap)).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
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
        private static Item from(AdminSystemSettingHistoryListResDto item, Map<Long, String> adminNameMap) {
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
}
