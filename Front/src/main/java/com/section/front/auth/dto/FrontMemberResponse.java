package com.section.front.auth.dto;

import com.section.front.auth.support.FrontMemberSession.AuthenticatedFrontMember;

public record FrontMemberResponse(
        boolean authenticated,
        Long memberId,
        String email,
        String name,
        String nickname
) {
    public static FrontMemberResponse authenticated(AuthenticatedFrontMember member) {
        return new FrontMemberResponse(
                true,
                member.memberId(),
                member.email(),
                member.name(),
                member.nickname()
        );
    }

    public static FrontMemberResponse anonymous() {
        return new FrontMemberResponse(false, null, null, null, null);
    }
}
