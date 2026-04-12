package com.section.admin.product.req;

import com.section.common.content.entity.Document;

public record ContentSaveRequest(
        Long id,
        String boardType,
        String title,
        String content,
        Long productNo
) {
    public Document toEntity() {
        Document document = new Document();
        document.setId(id);
        document.setBoardType(Document.BoardType.valueOf(boardType));
        document.setTitle(title);
        document.setContent(content);
        document.setProductNo(productNo);
        return document;
    }
}
