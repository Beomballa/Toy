package com.section.front.content.req;

import com.section.common.content.entity.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Locale;

public record FrontContentListRequest(
        String boardType,
        String keyword,
        Integer page,
        Integer size
) {

    private static final int DEFAULT_SIZE = 8;
    private static final int MAX_SIZE = 20;
    private static final int MAX_KEYWORD_LENGTH = 100;

    public Document.BoardType normalizedBoardType() {
        if (boardType == null || boardType.isBlank() || "ALL".equalsIgnoreCase(boardType.trim())) {
            return null;
        }
        String normalized = boardType.trim().toUpperCase(Locale.ROOT);
        if (!"NOTICE".equals(normalized) && !"STYLE".equals(normalized)) {
            throw new IllegalArgumentException("공개 콘텐츠 게시판 유형이 올바르지 않습니다.");
        }
        return Document.BoardType.valueOf(normalized);
    }

    public String normalizedKeyword() {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String normalized = keyword.trim();
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new IllegalArgumentException("콘텐츠 검색어는 100자 이하여야 합니다.");
        }
        return normalized;
    }

    public Pageable pageable() {
        int normalizedPage = page == null ? 0 : page;
        int normalizedSize = size == null ? DEFAULT_SIZE : size;
        if (normalizedPage < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        if (normalizedSize < 1 || normalizedSize > MAX_SIZE) {
            throw new IllegalArgumentException("페이지 크기는 1개 이상 20개 이하여야 합니다.");
        }
        return PageRequest.of(normalizedPage, normalizedSize);
    }
}
