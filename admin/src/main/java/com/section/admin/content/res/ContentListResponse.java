package com.section.admin.content.res;

import com.section.common.content.entity.Document;
import org.springframework.data.domain.Page;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record ContentListResponse(
    List<ContentItem> items,
    long totalElements,
    int totalPages,
    int currentPage
) {
    public record ContentItem(
        Long id,
        String boardType,
        String title,
        String contentPreview,
        int viewCnt,
        String crtDtm
    ) {}

    public static ContentListResponse of(Page<Document> page) {
        List<ContentItem> items = page.getContent().stream()
            .map(d -> new ContentItem(
                d.getId(),
                d.getBoardType().name(),
                d.getTitle(),
                buildPreview(d.getContent()),
                d.getViewCnt(),
                d.getCrtDtm() != null ? d.getCrtDtm().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : ""
            )).toList();
        
        return new ContentListResponse(
            items,
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber()
        );
    }

    private static String buildPreview(String content) {
        if (content == null || content.isBlank()) {
            return "내용 미리보기가 없습니다.";
        }

        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 88) {
            return normalized;
        }
        return normalized.substring(0, 88) + "...";
    }
}
