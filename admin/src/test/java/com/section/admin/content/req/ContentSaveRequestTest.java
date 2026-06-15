package com.section.admin.content.req;

import com.section.common.base.entity.type.YN;
import com.section.common.base.exception.BusinessException;
import com.section.common.content.entity.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentSaveRequestTest {

    @Test
    @DisplayName("콘텐츠 저장 요청은 게시 타입과 상태, 공개값을 대소문자와 공백 기준으로 정규화한다")
    void toEntityNormalizesEnumStyleFields() {
        ContentSaveRequest request = new ContentSaveRequest(
                1L,
                " discuss ",
                " 제목 ",
                " 본문 ",
                7L,
                " published ",
                " n ",
                " y "
        );

        Document document = request.toEntity();

        assertEquals(Document.BoardType.DISCUSS, document.getBoardType());
        assertEquals(Document.PublishStatus.PUBLISHED, document.getStatus());
        assertEquals(YN.N, document.getPublicYn());
        assertEquals(YN.Y, document.getPinnedYn());
        assertEquals("제목", document.getTitle());
        assertEquals("본문", document.getContent());
        assertEquals(7L, document.getProductNo());
    }

    @Test
    @DisplayName("콘텐츠 저장 요청은 잘못된 게시 타입을 거부한다")
    void toEntityRejectsInvalidBoardType() {
        ContentSaveRequest request = new ContentSaveRequest(
                null,
                "unknown",
                "제목",
                "본문",
                null,
                null,
                null,
                null
        );

        assertThrows(BusinessException.class, request::toEntity);
    }
}
