package com.section.admin.user.res;

import com.section.common.base.entity.type.YN;
import com.section.common.system.dto.AccountListQuery;
import com.section.common.system.dto.AccountListResDto;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminMemberListResponse(
        List<Item> items,
        long totalElements,
        int totalPages,
        long rangeStart,
        long rangeEnd,
        AppliedQuery appliedQuery
) {
    public static AdminMemberListResponse of(Page<AccountListResDto> page, AccountListQuery query) {
        long rangeStart = page.getTotalElements() == 0 ? 0 : page.getNumber() * page.getSize() + 1L;
        long rangeEnd = page.getTotalElements() == 0 ? 0 : Math.min(page.getTotalElements(), rangeStart + page.getNumberOfElements() - 1L);
        return new AdminMemberListResponse(
                page.getContent().stream().map(Item::from).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                rangeStart,
                rangeEnd,
                new AppliedQuery(query.keyword(), ynName(query.masterYn()), ynName(query.delYn()))
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
}
