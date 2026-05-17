package com.section.admin.user.res;

import com.section.common.base.entity.type.YN;
import com.section.common.system.dto.AccountListQuery;
import com.section.common.system.dto.AccountListResDto;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminMemberListResponse(
        List<Item> items,
        int currentPage,
        int pageSize,
        long totalElements,
        int totalPages,
        long rangeStart,
        long rangeEnd,
        AppliedQuery appliedQuery,
        ResultMeta resultMeta
) {
    public static AdminMemberListResponse of(Page<AccountListResDto> page, AccountListQuery query) {
        long rangeStart = page.getTotalElements() == 0 ? 0 : page.getNumber() * page.getSize() + 1L;
        long rangeEnd = page.getTotalElements() == 0 ? 0 : Math.min(page.getTotalElements(), rangeStart + page.getNumberOfElements() - 1L);
        return new AdminMemberListResponse(
                page.getContent().stream().map(Item::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                rangeStart,
                rangeEnd,
                new AppliedQuery(query.keyword(), ynName(query.masterYn()), ynName(query.delYn())),
                ResultMeta.from(page, query, rangeStart, rangeEnd)
        );
    }

    private static String ynName(YN value) {
        return value == null ? null : value.name();
    }

    public record Item(
            Long id,
            String email,
            String name,
            String nickname,
            String masterYn,
            String initYn,
            String delYn,
            String crtDtm
    ) {
        public static Item from(AccountListResDto item) {
            return new Item(
                    item.getId(),
                    item.getEmail(),
                    item.getName(),
                    item.getNickname(),
                    item.getMasterYn() == null ? null : item.getMasterYn().name(),
                    item.getInitYn() == null ? null : item.getInitYn().name(),
                    item.getDelYn() == null ? null : item.getDelYn().name(),
                    item.getCrtDtm() == null ? "-" : item.getCrtDtm().toString().replace('T', ' ')
            );
        }
    }

    public record AppliedQuery(
            String keyword,
            String masterYn,
            String delYn
    ) {
    }

    public record ResultMeta(
            String resultLabel,
            String pageInfoLabel,
            long appliedFilterCount,
            boolean hasActiveFilters,
            String querySignature
    ) {
        private static ResultMeta from(Page<?> page, AccountListQuery query, long rangeStart, long rangeEnd) {
            long filterCount = appliedFilterCount(query);
            boolean hasActiveFilters = filterCount > 0;
            String resultLabel = hasActiveFilters
                    ? String.format("검색 결과 %,d명", page.getTotalElements())
                    : String.format("전체 %,d명", page.getTotalElements());
            String pageInfoLabel = page.getTotalElements() == 0
                    ? "조건에 맞는 회원이 없습니다."
                    : String.format("%d-%d / %,d명 · %d페이지", rangeStart, rangeEnd, page.getTotalElements(), Math.max(page.getTotalPages(), 1));
            return new ResultMeta(
                    resultLabel,
                    pageInfoLabel,
                    filterCount,
                    hasActiveFilters,
                    querySignature(query)
            );
        }

        private static long appliedFilterCount(AccountListQuery query) {
            long count = 0;
            if (query.keyword() != null) count++;
            if (query.masterYn() != null) count++;
            if (query.delYn() != null) count++;
            return count;
        }

        private static String querySignature(AccountListQuery query) {
            StringBuilder builder = new StringBuilder("최신 가입순");
            if (query.keyword() != null) {
                builder.append(" · 검색=").append(query.keyword());
            }
            if (query.masterYn() != null) {
                builder.append(" · 권한=").append(query.masterYn().name());
            }
            if (query.delYn() != null) {
                builder.append(" · 상태=").append(query.delYn().name());
            }
            return builder.toString();
        }
    }
}
