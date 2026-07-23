package com.section.front.content.service;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.dto.DocumentListSort;
import com.section.common.content.dto.PopularPublicContentRow;
import com.section.common.content.dto.PublicDocumentRow;
import com.section.common.content.dto.PublicDocumentNavigationRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.front.content.dto.FrontContentHighlightsResponse;
import com.section.front.content.dto.FrontContentDetailResponse;
import com.section.front.content.dto.FrontContentItemResponse;
import com.section.front.content.dto.FrontContentNavigationResponse;
import com.section.front.content.dto.FrontContentPageResponse;
import com.section.front.content.dto.FrontPopularContentResponse;
import com.section.front.content.req.FrontContentListRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class FrontContentService {

    private static final int DEFAULT_LIMIT = 4;
    private static final int MAX_LIMIT = 8;
    private static final int SUMMARY_MAX_LENGTH = 140;
    private static final int READING_CHARACTERS_PER_MINUTE = 500;
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DocumentRepository documentRepository;
    private final Clock clock;

    @Autowired
    public FrontContentService(DocumentRepository documentRepository) {
        this(documentRepository, Clock.systemDefaultZone());
    }

    FrontContentService(DocumentRepository documentRepository, Clock clock) {
        this.documentRepository = documentRepository;
        this.clock = clock;
    }

    public FrontContentHighlightsResponse getHighlights(Integer limit) {
        int normalizedLimit = normalizeLimit(limit);
        LocalDate popularEndDate = LocalDate.now(clock);
        LocalDate popularStartDate = popularEndDate.minusDays(6);
        return new FrontContentHighlightsResponse(
                toResponses(documentRepository.getPublicDocuments(Document.BoardType.NOTICE, normalizedLimit)),
                toResponses(documentRepository.getPublicDocuments(Document.BoardType.STYLE, normalizedLimit)),
                documentRepository.getPopularPublicDocuments(popularStartDate, popularEndDate, normalizedLimit)
                        .stream()
                        .map(this::toPopularResponse)
                        .toList(),
                popularStartDate.toString(),
                popularEndDate.toString()
        );
    }

    public Optional<FrontContentDetailResponse> findDetail(long documentId) {
        return documentRepository.getPublicDocument(documentId)
                .map(this::toDetailResponse);
    }

    private FrontContentDetailResponse toDetailResponse(PublicDocumentRow row) {
        String content = plainText(row.content());
        int characterCount = content.codePointCount(0, content.length());
        int estimatedReadMinutes = Math.max(
                1,
                (int) Math.ceil(characterCount / (double) READING_CHARACTERS_PER_MINUTE)
        );
        return new FrontContentDetailResponse(
                row.id(),
                row.boardType().name(),
                defaultText(row.title(), "제목 없는 콘텐츠"),
                content,
                Math.max(0, row.viewCount()),
                row.pinnedYn() == YN.Y,
                formatDate(row),
                estimatedReadMinutes,
                characterCount,
                adjacentContent(row, true),
                adjacentContent(row, false),
                relatedContents(row)
        );
    }

    private FrontContentNavigationResponse adjacentContent(PublicDocumentRow row, boolean newer) {
        if (row.createdAt() == null) {
            return null;
        }
        Optional<PublicDocumentNavigationRow> adjacent = newer
                ? documentRepository.getNewerPublicDocument(row.boardType(), row.createdAt(), row.id())
                : documentRepository.getOlderPublicDocument(row.boardType(), row.createdAt(), row.id());
        return adjacent.map(this::toNavigationResponse).orElse(null);
    }

    private FrontContentNavigationResponse toNavigationResponse(PublicDocumentNavigationRow row) {
        return new FrontContentNavigationResponse(
                row.id(),
                row.boardType().name(),
                defaultText(row.title(), "제목 없는 콘텐츠"),
                row.createdAt() == null ? null : row.createdAt().toLocalDate().format(DATE_FORMATTER)
        );
    }

    public FrontContentPageResponse search(FrontContentListRequest request) {
        Pageable pageable = request.pageable();
        DocumentListSort sort = request.normalizedSort();
        Page<DocumentListItemDto> result = documentRepository.getDocumentList(
                new DocumentListQuery(
                        request.normalizedBoardType(),
                        request.normalizedKeyword(),
                        Document.PublishStatus.PUBLISHED,
                        YN.Y,
                        false,
                        null,
                        null,
                        null,
                        null,
                        sort
                ),
                pageable
        );
        List<FrontContentItemResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new FrontContentPageResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast(),
                sort.name(),
                items.stream().mapToLong(FrontContentItemResponse::viewCount).sum(),
                (int) items.stream().filter(FrontContentItemResponse::pinned).count(),
                (int) items.stream().filter(item -> "NOTICE".equals(item.boardType())).count(),
                (int) items.stream().filter(item -> "STYLE".equals(item.boardType())).count()
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
                formatDate(row)
        );
    }

    private FrontContentItemResponse toResponse(DocumentListItemDto row) {
        return new FrontContentItemResponse(
                row.getId(),
                row.getBoardType(),
                defaultText(row.getTitle(), "제목 없는 콘텐츠"),
                summarize(row.getContentPreview()),
                Math.max(0, row.getViewCnt()),
                "Y".equals(row.getPinnedYn()),
                row.getCrtDtm() == null ? null : row.getCrtDtm().toLocalDate().format(DATE_FORMATTER)
        );
    }

    private FrontPopularContentResponse toPopularResponse(PopularPublicContentRow row) {
        return new FrontPopularContentResponse(
                row.id(),
                row.boardType().name(),
                defaultText(row.title(), "제목 없는 콘텐츠"),
                summarize(row.content()),
                row.recentViewCount(),
                row.uniqueVisitors(),
                row.pinnedYn() == YN.Y,
                row.createdAt() == null ? null : row.createdAt().toLocalDate().format(DATE_FORMATTER)
        );
    }

    private List<FrontContentItemResponse> relatedContents(PublicDocumentRow current) {
        return documentRepository.getPublicDocuments(current.boardType(), 5).stream()
                .filter(row -> !row.id().equals(current.id()))
                .limit(4)
                .map(this::toResponse)
                .toList();
    }

    private String summarize(String content) {
        String normalized = plainText(content);
        if (normalized.length() <= SUMMARY_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, SUMMARY_MAX_LENGTH).trim() + "...";
    }

    private String plainText(String content) {
        return WHITESPACE.matcher(HTML_TAG.matcher(defaultText(content, "")).replaceAll(" "))
                .replaceAll(" ")
                .trim();
    }

    private String formatDate(PublicDocumentRow row) {
        return row.createdAt() == null ? null : row.createdAt().toLocalDate().format(DATE_FORMATTER);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
