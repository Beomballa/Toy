package com.section.admin.user.res;

import com.section.common.system.dto.AdminUserListQuery;
import com.section.common.system.dto.AdminUserListResDto;
import com.section.common.system.dto.AdminUserSummaryDto;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminUserListResponse(
        List<Item> items,
        Summary summary,
        int currentPage,
        int pageSize,
        long totalElements,
        int totalPages,
        long rangeStart,
        long rangeEnd,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static AdminUserListResponse of(
            Page<AdminUserListResDto> page,
            AdminUserListQuery query,
            AdminUserSummaryDto summary
    ) {
        long rangeStart = page.getTotalElements() == 0 ? 0 : page.getNumber() * page.getSize() + 1L;
        long rangeEnd = page.getTotalElements() == 0 ? 0 : Math.min(page.getTotalElements(), rangeStart + page.getNumberOfElements() - 1L);

        return new AdminUserListResponse(
                page.getContent().stream().map(Item::from).toList(),
                Summary.from(summary),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                rangeStart,
                rangeEnd,
                new AppliedQuery(query.keyword(), query.role(), query.status(), query.inactiveDays(), query.neverLoggedInOnly()),
                ResultMeta.from(page, query, rangeStart, rangeEnd)
        );
    }

    public record Item(
            Long adminNo,
            String loginId,
            String name,
            String role,
            String roleLabel,
            String status,
            String statusLabel,
            String lastLoginDtm,
            String crtDtm
    ) {
        private static Item from(AdminUserListResDto item) {
            return new Item(
                    item.getAdminNo(),
                    item.getLoginId(),
                    item.getName(),
                    item.getRole(),
                    "ROLE_SUPER".equals(item.getRole()) ? "최고 관리자" : "일반 관리자",
                    item.getStatus(),
                    "SUSPENDED".equals(item.getStatus()) ? "정지" : "활성",
                    formatDateTime(item.getLastLoginDtm()),
                    formatDateTime(item.getCrtDtm())
            );
        }
    }

    public record Summary(
            long totalCount,
            long activeCount,
            long suspendedCount,
            long superCount,
            long inactiveCount
    ) {
        private static Summary from(AdminUserSummaryDto dto) {
            return new Summary(
                    dto.totalCount(),
                    dto.activeCount(),
                    dto.suspendedCount(),
                    dto.superCount(),
                    dto.inactiveCount()
            );
        }
    }

    public record AppliedQuery(
            String keyword,
            String role,
            String status,
            Integer inactiveDays,
            Boolean neverLoggedInOnly
    ) {
    }

    public record ResultMeta(
            String resultLabel,
            String pageInfoLabel,
            long appliedFilterCount,
            boolean hasActiveFilters,
            String querySignature
    ) {
        private static ResultMeta from(Page<?> page, AdminUserListQuery query, long rangeStart, long rangeEnd) {
            long filterCount = appliedFilterCount(query);
            boolean hasActiveFilters = filterCount > 0;
            String resultLabel = hasActiveFilters
                    ? String.format("검색 결과 %,d명", page.getTotalElements())
                    : String.format("전체 %,d명", page.getTotalElements());
            String pageInfoLabel = page.getTotalElements() == 0
                    ? "조건에 맞는 관리자가 없습니다."
                    : String.format("%d-%d / %,d명 · %d페이지", rangeStart, rangeEnd, page.getTotalElements(), Math.max(page.getTotalPages(), 1));
            return new ResultMeta(
                    resultLabel,
                    pageInfoLabel,
                    filterCount,
                    hasActiveFilters,
                    querySignature(query)
            );
        }

        private static long appliedFilterCount(AdminUserListQuery query) {
            long count = 0;
            if (query.keyword() != null) count++;
            if (query.role() != null) count++;
            if (query.status() != null) count++;
            if (query.inactiveDays() != null) count++;
            if (Boolean.TRUE.equals(query.neverLoggedInOnly())) count++;
            return count;
        }

        private static String querySignature(AdminUserListQuery query) {
            StringBuilder builder = new StringBuilder("권한 우선 · 최근 로그인순");
            if (query.keyword() != null) {
                builder.append(" · 검색=").append(query.keyword());
            }
            if (query.role() != null) {
                builder.append(" · 권한=").append("ROLE_SUPER".equals(query.role()) ? "최고 관리자" : "일반 관리자");
            }
            if (query.status() != null) {
                builder.append(" · 상태=").append("SUSPENDED".equals(query.status()) ? "정지" : "활성");
            }
            if (query.inactiveDays() != null) {
                builder.append(" · 미접속 ").append(query.inactiveDays()).append("일+");
            }
            if (Boolean.TRUE.equals(query.neverLoggedInOnly())) {
                builder.append(" · 로그인 이력 없음");
            }
            return builder.toString();
        }
    }

    private static String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "-" : value.toString().replace('T', ' ');
    }
}
