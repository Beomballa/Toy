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
        int currentPage,
        int pageSize,
        long rangeStart,
        long rangeEnd,
        String pageInfoLabel,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static AdminLogListResponse of(Page<AdminActivityLogListResDto> page, AdminActivityLogListQuery query, Map<Long, String> adminNameMap) {
        long rangeStart = page.getTotalElements() == 0 ? 0 : page.getNumber() * page.getSize() + 1L;
        long rangeEnd = page.getTotalElements() == 0 ? 0 : Math.min(page.getTotalElements(), rangeStart + page.getNumberOfElements() - 1L);
        String pageInfoLabel = page.getTotalElements() == 0
                ? "조회 결과 없음"
                : "%d-%d / %d건 · %d페이지".formatted(rangeStart, rangeEnd, page.getTotalElements(), page.getNumber() + 1);
        return new AdminLogListResponse(
                page.getContent().stream().map(item -> Item.from(item, adminNameMap.getOrDefault(item.getAdminNo(), "관리자#" + item.getAdminNo()))).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                rangeStart,
                rangeEnd,
                pageInfoLabel,
                new AppliedQuery(query.adminNo(), query.actionType(), query.targetId(), query.startDate() == null ? null : query.startDate().toString(), query.endDate() == null ? null : query.endDate().toString()),
                ResultMeta.from(query, rangeStart, rangeEnd, pageInfoLabel, page.getTotalElements())
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

    public record ResultMeta(
            String resultLabel,
            String pageInfoLabel,
            int filterCount,
            String querySignature
    ) {
        public static ResultMeta from(
                AdminActivityLogListQuery query,
                long rangeStart,
                long rangeEnd,
                String pageInfoLabel,
                long totalElements
        ) {
            return new ResultMeta(
                    totalElements == 0 ? "조회 결과 없음" : "검색 결과 " + totalElements + "건",
                    pageInfoLabel,
                    countFilters(query),
                    buildQuerySignature(query, rangeStart, rangeEnd)
            );
        }

        private static int countFilters(AdminActivityLogListQuery query) {
            int count = 0;
            if (query.adminNo() != null) count += 1;
            if (query.actionType() != null && !query.actionType().isBlank()) count += 1;
            if (query.targetId() != null) count += 1;
            if (query.startDate() != null) count += 1;
            if (query.endDate() != null) count += 1;
            return count;
        }

        private static String buildQuerySignature(AdminActivityLogListQuery query, long rangeStart, long rangeEnd) {
            StringBuilder builder = new StringBuilder();
            builder.append(rangeStart).append("-").append(rangeEnd);
            if (query.adminNo() != null) builder.append(" · 관리자=").append(query.adminNo());
            if (query.actionType() != null && !query.actionType().isBlank()) builder.append(" · 작업=").append(query.actionType());
            if (query.targetId() != null) builder.append(" · 대상=").append(query.targetId());
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
