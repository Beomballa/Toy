package com.section.admin.log.res;

import com.section.common.system.dto.AdminActivityLogListQuery;
import com.section.common.system.dto.AdminActivityLogListResDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public record AdminLogListResponse(
        List<Item> items,
        long totalElements,
        int totalPages,
        long rangeStart,
        long rangeEnd,
        AppliedQuery appliedQuery
) {
    public static AdminLogListResponse of(Page<AdminActivityLogListResDto> page, AdminActivityLogListQuery query, Map<Long, String> adminNameMap) {
        long rangeStart = page.getTotalElements() == 0 ? 0 : page.getNumber() * page.getSize() + 1L;
        long rangeEnd = page.getTotalElements() == 0 ? 0 : Math.min(page.getTotalElements(), rangeStart + page.getNumberOfElements() - 1L);
        return new AdminLogListResponse(
                page.getContent().stream().map(item -> Item.from(item, adminNameMap.getOrDefault(item.getAdminNo(), "관리자#" + item.getAdminNo()))).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                rangeStart,
                rangeEnd,
                new AppliedQuery(query.adminNo(), query.actionType(), query.targetId(), query.startDate() == null ? null : query.startDate().toString(), query.endDate() == null ? null : query.endDate().toString())
        );
    }

    public record Item(
            Long logNo,
            Long adminNo,
            String adminName,
            String actionType,
            Long targetId,
            String targetLabel,
            String targetPath,
            String ipAddress,
            String actionDtm
    ) {
        public static Item from(AdminActivityLogListResDto item, String adminName) {
            return new Item(
                    item.getLogNo(),
                    item.getAdminNo(),
                    adminName,
                    item.getActionType(),
                    item.getTargetId(),
                    AdminLogTargetLinkSupport.resolveTargetLabel(item.getActionType(), item.getTargetId()),
                    AdminLogTargetLinkSupport.resolveTargetPath(item.getActionType(), item.getTargetId()),
                    item.getIpAddress(),
                    item.getActionDtm() == null ? "-" : item.getActionDtm().toString().replace('T', ' ')
            );
        }
    }

    public record AppliedQuery(
            Long adminNo,
            String actionType,
            Long targetId,
            String startDate,
            String endDate
    ) {
    }
}
