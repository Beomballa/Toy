package com.section.admin.notice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminOperationNoticeControllerTest {

    private final AdminOperationNoticeController controller = new AdminOperationNoticeController();

    @Test
    @DisplayName("운영 공지 화면은 전용 뷰를 반환한다")
    void noticeListReturnsView() {
        assertEquals("views/notice-list", controller.noticeList());
    }
}
