package com.section.admin.notice.res;

import com.section.admin.log.res.AdminLogSourceLinkSupport;
import com.section.common.base.entity.type.AdminNoticeVisibilityStatus;
import com.section.common.system.dto.AdminOperationNoticeListQuery;
import com.section.common.system.dto.AdminOperationNoticeListResDto;
import com.section.common.system.dto.AdminOperationNoticeSummaryDto;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public record AdminOperationNoticeListResponse(
        List<Item> items,
        int currentPage,
        int totalPages,
        long totalElements,
        int pageSize,
        NoticeStats noticeStats,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static AdminOperationNoticeListResponse of(Page<AdminOperationNoticeListResDto> page, AdminOperationNoticeListQuery query, AdminOperationNoticeSummaryDto summary) {
        return new AdminOperationNoticeListResponse(
                page.getContent().stream().map(Item::from).toList(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                NoticeStats.from(summary, query.toStatsQuery()),
                new AppliedQuery(query.keyword(), query.isActive(), query.isPinned(), query.visibilityStatus() == null ? null : query.visibilityStatus().name()),
                ResultMeta.from(page, query)
        );
    }

    public record Item(
            Long noticeNo,
            String title,
            String content,
            String isActive,
            String isPinned,
            String displayStatus,
            String startDtm,
            String endDtm,
            String crtDtm,
            String historyPath,
            String activityLogPath,
            String activityLogLabel
    ) {
        public static Item from(AdminOperationNoticeListResDto item) {
            return new Item(
                    item.getNoticeNo(),
                    item.getTitle(),
                    item.getContent(),
                    item.getIsActive(),
                    item.getIsPinned(),
                    resolveDisplayStatus(item),
                    format(item.getStartDtm()),
                    format(item.getEndDtm()),
                    format(item.getCrtDtm()),
                    "/admin/settings/notices/history?noticeNo=" + item.getNoticeNo(),
                    AdminLogSourceLinkSupport.resolveNoticeLogPath(item.getNoticeNo()),
                    "활동 로그"
            );
        }

        private static String resolveDisplayStatus(AdminOperationNoticeListResDto item) {
            if (!"Y".equalsIgnoreCase(item.getIsActive())) {
                return "비활성";
            }
            LocalDateTime now = LocalDateTime.now();
            if (item.getStartDtm() != null && item.getStartDtm().isAfter(now)) {
                return "예약";
            }
            if (item.getEndDtm() != null && item.getEndDtm().isBefore(now)) {
                return "종료";
            }
            return "노출중";
        }

        private static String format(LocalDateTime value) {
            return value == null ? "-" : value.toString().replace('T', ' ');
        }
    }

    public record AppliedQuery(
            String keyword,
            String isActive,
            String isPinned,
            String visibilityStatus
    ) {}

    public record NoticeStats(
            long totalCount,
            long liveCount,
            long scheduledCount,
            long pinnedCount,
            String contextLabel,
            String querySignature
    ) {
        static NoticeStats from(AdminOperationNoticeSummaryDto summary, AdminOperationNoticeListQuery query) {
            return new NoticeStats(
                    summary.totalCount(),
                    summary.liveCount(),
                    summary.scheduledCount(),
                    summary.pinnedCount(),
                    buildContextLabel(query),
                    buildQuerySignature(query)
            );
        }

        private static String buildContextLabel(AdminOperationNoticeListQuery query) {
            return query.keyword() == null
                    && query.isActive() == null
                    ? "기본 문맥 기준"
                    : "검색 문맥 기준";
        }

        private static String buildQuerySignature(AdminOperationNoticeListQuery query) {
            StringBuilder builder = new StringBuilder("고정 우선 최신순");
            if (query.keyword() != null) {
                builder.append(" · 검색=").append(query.keyword());
            }
            if (query.isActive() != null) {
                builder.append(" · 상태=").append("Y".equals(query.isActive()) ? "활성" : "비활성");
            }
            return builder.toString();
        }
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
        public static ResultMeta from(Page<AdminOperationNoticeListResDto> page, AdminOperationNoticeListQuery query) {
            long totalElements = page.getTotalElements();
            long rangeStart = totalElements == 0 ? 0 : (long) page.getNumber() * page.getSize() + 1;
            long rangeEnd = totalElements == 0 ? 0 : rangeStart + page.getNumberOfElements() - 1;
            long filterCount = countFilters(query);
            boolean hasActiveFilters = filterCount > 0;

            return new ResultMeta(
                    hasActiveFilters ? "검색 결과 %d건".formatted(totalElements) : "전체 %d건".formatted(totalElements),
                    totalElements == 0
                            ? "조건에 맞는 운영 공지가 없습니다."
                            : "%d-%d / %d건 · %d페이지".formatted(rangeStart, rangeEnd, totalElements, Math.max(page.getTotalPages(), 1)),
                    filterCount,
                    hasActiveFilters,
                    querySignature(query),
                    rangeStart,
                    rangeEnd
            );
        }

        private static long countFilters(AdminOperationNoticeListQuery query) {
            long count = 0;
            if (query.keyword() != null) count++;
            if (query.isActive() != null) count++;
            if (query.isPinned() != null) count++;
            if (query.visibilityStatus() != null) count++;
            return count;
        }

        private static String querySignature(AdminOperationNoticeListQuery query) {
            StringBuilder builder = new StringBuilder("고정 우선 최신순");
            if (query.keyword() != null) {
                builder.append(" · 검색=").append(query.keyword());
            }
            if (query.isActive() != null) {
                builder.append(" · 상태=").append("Y".equals(query.isActive()) ? "활성" : "비활성");
            }
            if (query.isPinned() != null) {
                builder.append(" · 고정=").append("Y".equals(query.isPinned()) ? "고정" : "일반");
            }
            if (query.visibilityStatus() != null) {
                builder.append(" · 노출=").append(query.visibilityStatus().label());
            }
            return builder.toString();
        }
    }
}
