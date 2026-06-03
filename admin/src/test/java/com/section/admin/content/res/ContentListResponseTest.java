package com.section.admin.content.res;

import com.section.common.content.dto.DocumentListItemDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentListResponseTest {

    @Test
    @DisplayName("콘텐츠 미리보기는 HTML 태그와 스크립트를 제거한 뒤 잘라낸다")
    void buildPreviewStripsHtmlAndScripts() {
        DocumentListItemDto item = new DocumentListItemDto();
        item.setId(1L);
        item.setBoardType("NOTICE");
        item.setStatus("PUBLISHED");
        item.setPublicYn("Y");
        item.setPinnedYn("N");
        item.setTitle("공지");
        item.setContentPreview("<script>alert('x')</script><p> 첫 <strong>문장</strong> &nbsp;둘째 문장입니다.</p>");
        item.setViewCnt(3);
        item.setCrtDtm(LocalDateTime.of(2026, 6, 1, 10, 0));

        ContentListResponse response = ContentListResponse.of(new PageImpl<>(List.of(item)));

        assertEquals("첫 문장 둘째 문장입니다.", response.items().get(0).contentPreview());
    }
}
