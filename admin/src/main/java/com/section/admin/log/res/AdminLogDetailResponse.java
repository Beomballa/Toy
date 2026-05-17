package com.section.admin.log.res;

import com.section.common.system.entity.AdminActivityLog;

public record AdminLogDetailResponse(
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
    public static AdminLogDetailResponse from(AdminActivityLog log, String adminName) {
        return new AdminLogDetailResponse(
                log.getLogNo(),
                log.getAdminNo(),
                adminName,
                log.getActionType(),
                log.getTargetId(),
                AdminLogTargetLinkSupport.resolveTargetLabel(log.getActionType(), log.getTargetId()),
                AdminLogTargetLinkSupport.resolveTargetPath(log.getActionType(), log.getTargetId()),
                log.getIpAddress(),
                log.getActionDtm() == null ? "-" : log.getActionDtm().toString().replace('T', ' ')
        );
    }
}
