package com.section.admin.content.res;

public record ContentSaveResponse(
        Long id
) {
    public static ContentSaveResponse from(Long id) {
        return new ContentSaveResponse(id);
    }
}
