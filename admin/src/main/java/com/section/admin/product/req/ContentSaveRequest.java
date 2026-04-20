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
        document.setId(this.id());
        document.setBoardType(Document.BoardType.valueOf(this.boardType()));
        document.setTitle(this.title());
        document.setContent(this.content());
        document.setProductNo(this.productNo());
        return document;
    }
}
