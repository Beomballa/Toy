package com.section.admin.content.req;

import com.section.common.base.entity.type.YN;
import com.section.common.base.exception.BusinessException;
import com.section.common.content.entity.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentBulkOperateRequestTest {

    @Test
    @DisplayName("커뮤니티 일괄 운영 요청은 선택 ID와 상태 값을 정규화한다")
    void normalizeBulkOperateRequest() {
        ContentBulkOperateRequest request = new ContentBulkOperateRequest();
        request.setIds(List.of(1L, 2L, 2L));
        request.setStatus("published");
        request.setPublicYn("n");
        request.setPinnedYn("y");

        assertEquals(2, request.normalizedIds().size());
        assertEquals(Document.PublishStatus.PUBLISHED, request.normalizedStatus());
        assertEquals(YN.N, request.normalizedPublicYn());
        assertEquals(YN.Y, request.normalizedPinnedYn());
        assertTrue(request.hasOperateField());
    }

    @Test
    @DisplayName("커뮤니티 일괄 운영 요청은 잘못된 상태값이면 INVALID_INPUT_VALUE 예외를 던진다")
    void normalizeBulkOperateRequestThrowsWhenStatusInvalid() {
        ContentBulkOperateRequest request = new ContentBulkOperateRequest();
        request.setIds(List.of(1L));
        request.setStatus("unknown");

        assertThrows(BusinessException.class, request::normalizedStatus);
    }
}
