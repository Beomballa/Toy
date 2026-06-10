package com.section.admin.task.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminTaskLinkSupportTest {

    @Test
    @DisplayName("운영 작업 링크 지원은 목록 모달 딥링크를 만든다")
    void buildListOpenPathBuildsDeepLink() {
        String path = AdminTaskLinkSupport.buildListOpenPath(14L, "/admin/settings/tasks?status=TODO", "task-list-row-title");

        assertEquals(
                "/admin/settings/tasks?taskNo=14&openTaskNo=14&focusTaskNo=14&returnTo=%2Fadmin%2Fsettings%2Ftasks%3Fstatus%3DTODO&source=task-list-row-title",
                path
        );
    }
}
