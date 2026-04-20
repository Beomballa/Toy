package com.section.admin.product.res;

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
        int viewCnt,
        String crtDtm
    ) {}

    public static ContentListResponse of(Page<Document> page) {
        List<ContentItem> items = page.getContent().stream()
            .map(d -> new ContentItem(
                d.getId(),
                d.getBoardType().name(),
                d.getTitle(),
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
}
