package com.section.admin.log.res;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminLogTargetLinkSupportTest {

    @Test
    @DisplayName("활동 로그 대상 경로는 운영 작업을 목록 딥링크로 연결한다")
    void resolveTargetPathMapsTaskToListDeepLink() {
        String path = AdminLogTargetLinkSupport.resolveTargetPath("TASK_UPDATE", 15L);

        assertEquals("/admin/settings/tasks?taskNo=15&openTaskNo=15&focusTaskNo=15&returnTo=%2Fadmin%2Fsettings%2Flogs&source=activity-log-task", path);
    }
}
