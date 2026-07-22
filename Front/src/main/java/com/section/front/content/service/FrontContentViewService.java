package com.section.front.content.service;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.PublicDocumentRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.content.repository.FrontContentViewEventRepository;
import com.section.front.content.dto.FrontContentViewResponse;
import com.section.front.content.exception.FrontContentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FrontContentViewService {

    private static final Pattern VISITOR_KEY_PATTERN = Pattern.compile("[A-Za-z0-9-]{16,64}");

    private final DocumentRepository documentRepository;
    private final FrontContentViewEventRepository viewEventRepository;
    private final Clock clock = Clock.systemDefaultZone();

    @Transactional
    public FrontContentViewResponse record(long documentId, String visitorKey) {
        String normalizedVisitorKey = normalizeVisitorKey(visitorKey);
        PublicDocumentRow document = documentRepository.getPublicDocument(documentId)
                .orElseThrow(FrontContentNotFoundException::new);
        LocalDateTime now = LocalDateTime.now(clock);
        int inserted = viewEventRepository.insertIfAbsent(
                documentId,
                normalizedVisitorKey,
                now.toLocalDate(),
                now
        );
        if (inserted > 0) {
            documentRepository.incrementPublicViewCount(
                    documentId,
                    Document.PublishStatus.PUBLISHED,
                    YN.Y
            );
        }
        int currentViewCount = Math.max(0, document.viewCount()) + (inserted > 0 ? 1 : 0);
        return new FrontContentViewResponse(inserted > 0, currentViewCount);
    }

    private String normalizeVisitorKey(String visitorKey) {
        if (visitorKey == null) {
            throw new IllegalArgumentException("방문자 키가 필요합니다.");
        }
        String normalized = visitorKey.trim();
        if (!VISITOR_KEY_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("방문자 키 형식이 올바르지 않습니다.");
        }
        return normalized;
    }
}
