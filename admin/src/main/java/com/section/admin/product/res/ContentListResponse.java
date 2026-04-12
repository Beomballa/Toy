package com.section.admin.product.res;

import com.section.common.content.entity.Document;
import com.section.common.util.DateUtil;
import org.springframework.data.domain.Page;

import java.util.List;

public record ContentListResponse(
        List<ContentItem> contents,
        int currentPage,
        int totalPages,
        long totalElements
) {
    public static ContentListResponse of(Page<Document> page) {
        return new ContentListResponse(
                page.getContent().stream().map(ContentItem::from).toList(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements()
        );
    }

    public record ContentItem(
            Long id,
            String boardType,
            String title,
            int viewCnt,
            String crtDtm
    ) {
        public static ContentItem from(Document doc) {
            return new ContentItem(
                    doc.getId(),
                    doc.getBoardType().name(),
                    doc.getTitle(),
                    doc.getViewCnt(),
                    doc.getCrtDtm() != null ? DateUtil.localDateTimeToStr(doc.getCrtDtm()) : ""
            );
        }
    }
}
