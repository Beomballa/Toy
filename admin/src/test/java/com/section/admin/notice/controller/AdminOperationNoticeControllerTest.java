package com.section.admin.notice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminOperationNoticeControllerTest {

    private final AdminOperationNoticeController controller = new AdminOperationNoticeController();

    @Test
    @DisplayName("운영 공지 화면은 전용 뷰를 반환한다")
    void noticeListReturnsView() {
        assertEquals("views/notice-list", controller.noticeList());
    }

    @Test
    @DisplayName("운영 공지 이력 화면은 전용 뷰를 반환한다")
    void noticeHistoryReturnsView() {
        assertEquals("views/notice-history", controller.noticeHistory());
    }

    @Test
    @DisplayName("운영 공지 상세 화면은 공지 번호와 복귀 경로를 모델에 담는다")
    void noticeDetailReturnsViewWithModel() {
        Model model = new ExtendedModelMap();

        String viewName = controller.noticeDetail(7L, "/admin/settings/notices?page=1", model);

        assertEquals("views/notice-get", viewName);
        assertEquals(7L, model.getAttribute("noticeNo"));
        assertEquals("/admin/settings/notices?page=1", model.getAttribute("returnTo"));
    }
}
