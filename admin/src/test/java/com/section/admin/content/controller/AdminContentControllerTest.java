package com.section.admin.content.controller;

import com.section.common.content.entity.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminContentControllerTest {

    private final AdminContentController adminContentController = new AdminContentController();

    @Test
    @DisplayName("콘텐츠 편집 화면은 게시판 타입과 게시 상태 목록을 뷰에 전달한다")
    void contentEditAddsBoardAndStatusEnumsToModel() {
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = adminContentController.contentEdit(3L, "DISCUSS", model);

        assertEquals("views/content-edit", viewName);
        assertEquals(3L, model.get("id"));
        assertEquals("DISCUSS", model.get("boardType"));
        assertArrayEquals(Document.BoardType.values(), (Document.BoardType[]) model.get("boardTypes"));
        assertArrayEquals(Document.PublishStatus.values(), (Document.PublishStatus[]) model.get("publishStatuses"));
    }
}
