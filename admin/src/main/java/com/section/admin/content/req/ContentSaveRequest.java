package com.section.admin.content.req;

import com.section.common.base.entity.type.YN;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.entity.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

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
    Long productNo,
    String status,
    String publicYn,
    String pinnedYn
) {
    public Document toEntity() {
        Document document = new Document();
        document.setId(this.id());
        document.applyEditorValues(
                parseBoardType(this.boardType()),
                parseStatus(this.status()),
                parseYn(this.publicYn(), YN.Y),
                parseYn(this.pinnedYn(), YN.N),
                this.title().trim(),
                this.content().trim(),
                this.productNo()
        );
        return document;
    }

    private Document.BoardType parseBoardType(String boardType) {
        try {
            return Document.BoardType.valueOf(normalizeEnumValue(boardType));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private Document.PublishStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return Document.PublishStatus.DRAFT;
        }

        try {
            return Document.PublishStatus.valueOf(normalizeEnumValue(status));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private YN parseYn(String value, YN defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return YN.valueOf(normalizeEnumValue(value));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String normalizeEnumValue(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
