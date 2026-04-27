package com.section.admin.content.res;

import com.section.common.content.entity.Document;

import java.time.format.DateTimeFormatter;

public record ContentDetailResponse(
        Long id,
        String boardType,
        String title,
        String content,
        int viewCnt,
        Long productNo,
        String crtDtm,
        String uptDtm
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static ContentDetailResponse from(Document document) {
        return new ContentDetailResponse(
                document.getId(),
                document.getBoardType() != null ? document.getBoardType().name() : "NOTICE",
                document.getTitle(),
                document.getContent(),
                document.getViewCnt(),
                document.getProductNo(),
                document.getCrtDtm() != null ? document.getCrtDtm().format(FORMATTER) : "",
                document.getUptDtm() != null ? document.getUptDtm().format(FORMATTER) : ""
        );
    }
}
