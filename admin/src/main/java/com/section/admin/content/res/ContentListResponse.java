package com.section.admin.content.res;

import com.section.common.content.dto.DocumentListItemDto;
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
        String status,
        String publicYn,
        String pinnedYn,
        String title,
        String contentPreview,
        int viewCnt,
        String crtDtm
    ) {}

    public static ContentListResponse of(Page<DocumentListItemDto> page) {
        List<ContentItem> items = page.getContent().stream()
            .map(d -> new ContentItem(
                d.getId(),
                d.getBoardType(),
                d.getStatus(),
                d.getPublicYn(),
                d.getPinnedYn(),
                d.getTitle(),
                buildPreview(d.getContentPreview()),
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
