package com.section.admin.content.support;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.entity.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public record ContentExportSummary(
        String exportedAt,
        String sortLabel,
        String filterSummary
) {
    private static final DateTimeFormatter EXPORTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static ContentExportSummary from(DocumentListQuery query) {
        List<String> filters = new ArrayList<>();
        if (query.boardType() != null) {
            filters.add("게시판: " + formatBoardType(query.boardType()));
        }
        if (query.keyword() != null) {
            filters.add("검색어: " + query.keyword());
        }
        if (query.status() != null) {
            filters.add("상태: " + formatStatus(query.status()));
        }
        if (query.publicYn() != null) {
            filters.add("공개여부: " + formatYn(query.publicYn()));
        }
        if (Boolean.TRUE.equals(query.pinnedOnly())) {
            filters.add("고정만");
        }
        if (query.startDateTime() != null || query.endDateTime() != null) {
            String start = query.startDateTime() == null ? "-" : query.startDateTime().toLocalDate().format(DATE_FORMAT);
            String end = query.endDateTime() == null ? "-" : query.endDateTime().toLocalDate().format(DATE_FORMAT);
            filters.add("등록일: " + start + " ~ " + end);
        }

        return new ContentExportSummary(
                LocalDateTime.now().format(EXPORTED_AT_FORMAT),
                "고정 우선 · 최신 등록 순",
                filters.isEmpty() ? "전체" : String.join(" | ", filters)
        );
    }

    private static String formatYn(YN value) {
        return value == YN.Y ? "공개" : "비공개";
    }

    private static String formatBoardType(Document.BoardType value) {
        return switch (value) {
            case NOTICE -> "공지";
            case STYLE -> "스타일";
            case DISCUSS -> "토론";
            case QNA -> "문의";
        };
    }

    private static String formatStatus(Document.PublishStatus value) {
        return switch (value) {
            case DRAFT -> "임시저장";
            case PUBLISHED -> "게시중";
        };
    }
}
