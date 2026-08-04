package com.section.front.auth.support;

import jakarta.servlet.http.HttpSession;

public final class FrontMemberSession {

    public static final String MEMBER_NO = "frontMemberNo";
    public static final String MEMBER_EMAIL = "frontMemberEmail";
    public static final String MEMBER_NAME = "frontMemberName";
    public static final String MEMBER_NICKNAME = "frontMemberNickname";

    private FrontMemberSession() {
    }

    public static void store(HttpSession session, AuthenticatedFrontMember member) {
        session.setAttribute(MEMBER_NO, member.memberId());
        session.setAttribute(MEMBER_EMAIL, member.email());
        session.setAttribute(MEMBER_NAME, member.name());
        session.setAttribute(MEMBER_NICKNAME, member.nickname());
    }

    public static AuthenticatedFrontMember read(HttpSession session) {
        if (session == null || !(session.getAttribute(MEMBER_NO) instanceof Long memberId)) {
            return null;
        }
        return new AuthenticatedFrontMember(
                memberId,
                text(session.getAttribute(MEMBER_EMAIL)),
                text(session.getAttribute(MEMBER_NAME)),
                text(session.getAttribute(MEMBER_NICKNAME))
        );
    }

    private static String text(Object value) {
        return value instanceof String text ? text : "";
    }

    public record AuthenticatedFrontMember(long memberId, String email, String name, String nickname) {
    }
}
