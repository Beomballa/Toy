package com.section.admin.user.res;

import com.section.common.system.entity.Account;

public record AdminMemberDetailResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String masterYn,
        String initYn,
        String delYn,
        String profileImgPath,
        String crtDtm
) {
    public static AdminMemberDetailResponse from(Account account) {
        return new AdminMemberDetailResponse(
                account.getId(),
                account.getEmail(),
                account.getName(),
                account.getNickname(),
                account.getMasterYn() == null ? null : account.getMasterYn().name(),
                account.getInitYn() == null ? null : account.getInitYn().name(),
                account.getDelYn() == null ? null : account.getDelYn().name(),
                account.getProfileImgPath(),
                account.getCrtDtm() == null ? "-" : account.getCrtDtm().toString().replace('T', ' ')
        );
    }
}
