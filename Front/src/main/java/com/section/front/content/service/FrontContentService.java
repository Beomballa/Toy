package com.section.front.content.service;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.PublicDocumentRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.front.content.dto.FrontContentHighlightsResponse;
import com.section.front.content.dto.FrontContentItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FrontContentService {

    private static final int DEFAULT_LIMIT = 4;
    private static final int MAX_LIMIT = 8;
    private static final int SUMMARY_MAX_LENGTH = 140;
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DocumentRepository documentRepository;

    public FrontContentHighlightsResponse getHighlights(Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        return new FrontContentHighlightsResponse(
                toResponses(documentRepository.getPublicDocuments(Document.BoardType.NOTICE, normalizedLimit)),
                toResponses(documentRepository.getPublicDocuments(Document.BoardType.STYLE, normalizedLimit))
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("콘텐츠 노출 개수는 1개 이상 8개 이하여야 합니다.");
        }
        return limit;
    }

    private List<FrontContentItemResponse> toResponses(List<PublicDocumentRow> rows) {
        return rows.stream().map(this::toResponse).toList();
    }

    private FrontContentItemResponse toResponse(PublicDocumentRow row) {
        return new FrontContentItemResponse(
                row.id(),
                row.boardType().name(),
                defaultText(row.title(), "제목 없는 콘텐츠"),
                summarize(row.content()),
                Math.max(0, row.viewCount()),
                row.pinnedYn() == YN.Y,
                row.createdAt() == null ? null : row.createdAt().toLocalDate().format(DATE_FORMATTER)
        );
    }

    private String summarize(String content) {
        String plainText = WHITESPACE.matcher(HTML_TAG.matcher(defaultText(content, "")).replaceAll(" "))
                .replaceAll(" ")
                .trim();
        if (plainText.length() <= SUMMARY_MAX_LENGTH) {
            return plainText;
        }
        return plainText.substring(0, SUMMARY_MAX_LENGTH).trim() + "...";
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
