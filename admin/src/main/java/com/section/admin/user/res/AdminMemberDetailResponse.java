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
        String tmpPwIssueDtm,
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
                account.getTmpPwIssueDt() == null ? "-" : account.getTmpPwIssueDt().toString().replace('T', ' '),
                resolveProfileImgPath(account),
                account.getCrtDtm() == null ? "-" : account.getCrtDtm().toString().replace('T', ' ')
        );
    }

    private static String resolveProfileImgPath(Account account) {
        String profileImgPath = normalize(account.getProfileImgPath());
        if (profileImgPath == null) {
            return null;
        }

        String profileImgName = normalize(account.getProfileImgName());
        if (profileImgName != null) {
            return profileImgPath.endsWith("/") ? profileImgPath + profileImgName : profileImgPath + "/" + profileImgName;
        }

        return isDirectAssetPath(profileImgPath) ? profileImgPath : null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static boolean isDirectAssetPath(String value) {
        int lastSlashIndex = value.lastIndexOf('/');
        String lastSegment = lastSlashIndex >= 0 ? value.substring(lastSlashIndex + 1) : value;
        return lastSegment.contains(".") || value.startsWith("http://") || value.startsWith("https://");
    }
}
