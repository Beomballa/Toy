package com.section.admin.content.support;

import com.section.common.content.dto.DocumentListItemDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentExportCsvWriterTest {

    @Test
    @DisplayName("콘텐츠 CSV 내보내기는 BOM과 한글 라벨, 정제된 미리보기를 포함한다")
    void writeIncludesBomAndSanitizedPreview() {
        DocumentListItemDto item = new DocumentListItemDto();
        item.setId(5L);
        item.setBoardType("NOTICE");
        item.setTitle("긴급 공지");
        item.setStatus("PUBLISHED");
        item.setPublicYn("Y");
        item.setPinnedYn("Y");
        item.setViewCnt(12);
        item.setCrtDtm(LocalDateTime.of(2026, 6, 1, 9, 30));
        item.setContentPreview("<p>안내 <strong>문구</strong> &amp; 점검</p>");

        byte[] bytes = ContentExportCsvWriter.write(
                new ContentExportSummary("2026.06.01 11:00", "고정 우선 · 최신 등록 순", "게시판: 공지 | 고정만"),
                List.of(item)
        );
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF);
        assertTrue(csv.contains("\"조회조건\",\"게시판: 공지 | 고정만\""));
        assertTrue(csv.contains("게시글번호,게시판,제목,상태,공개여부,고정여부,조회수,등록일시,내용미리보기"));
        assertTrue(csv.contains("\"공지\",\"긴급 공지\",\"게시중\""));
        assertTrue(csv.contains("\"안내 문구 & 점검\""));
    }
}
