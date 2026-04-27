package com.section.admin.content.req;

import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.entity.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContentSaveRequest(
    Long id,
    @NotBlank
    String boardType,
    @NotBlank
    @Size(max = 200)
    String title,
    @NotBlank
    @Size(max = 10000)
    String content,
    Long productNo
) {
    public Document toEntity() {
        Document document = new Document();
        document.setId(this.id());
        document.setBoardType(parseBoardType(this.boardType()));
        document.setTitle(this.title());
        document.setContent(this.content());
        document.setProductNo(this.productNo());
        return document;
    }

    private Document.BoardType parseBoardType(String boardType) {
        try {
            return Document.BoardType.valueOf(boardType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
