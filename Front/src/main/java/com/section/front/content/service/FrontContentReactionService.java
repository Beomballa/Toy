package com.section.front.content.service;

import com.section.common.content.dto.ContentReactionSummaryRow;
import com.section.common.content.entity.FrontContentReaction;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.content.repository.FrontContentReactionRepository;
import com.section.front.content.dto.FrontContentReactionResponse;
import com.section.front.content.exception.FrontContentNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class FrontContentReactionService {

    private static final Pattern VISITOR_KEY_PATTERN = Pattern.compile("[A-Za-z0-9-]{16,64}");

    private final DocumentRepository documentRepository;
    private final FrontContentReactionRepository reactionRepository;
    private final Clock clock;

    @Autowired
    public FrontContentReactionService(
            DocumentRepository documentRepository,
            FrontContentReactionRepository reactionRepository
    ) {
        this(documentRepository, reactionRepository, Clock.systemDefaultZone());
    }

    FrontContentReactionService(
            DocumentRepository documentRepository,
            FrontContentReactionRepository reactionRepository,
            Clock clock
    ) {
        this.documentRepository = documentRepository;
        this.reactionRepository = reactionRepository;
        this.clock = clock;
    }

    public FrontContentReactionResponse getSummary(long documentId, String visitorKey) {
        String normalizedVisitorKey = normalizeVisitorKey(visitorKey);
        assertPublicDocument(documentId);
        String selectedReaction = reactionRepository
                .findByDocumentNoAndVisitorKey(documentId, normalizedVisitorKey)
                .map(FrontContentReaction::getReactionType)
                .map(Enum::name)
                .orElse(null);
        return toResponse(documentId, selectedReaction, false);
    }

    @Transactional
    public FrontContentReactionResponse react(long documentId, String visitorKey, String reaction) {
        String normalizedVisitorKey = normalizeVisitorKey(visitorKey);
        FrontContentReaction.ReactionType normalizedReaction = normalizeReaction(reaction);
        assertPublicDocument(documentId);
        Optional<FrontContentReaction> previous =
                reactionRepository.findByDocumentNoAndVisitorKey(documentId, normalizedVisitorKey);
        boolean changed = previous
                .map(FrontContentReaction::getReactionType)
                .map(type -> type != normalizedReaction)
                .orElse(true);
        reactionRepository.upsert(
                documentId,
                normalizedVisitorKey,
                normalizedReaction.name(),
                LocalDateTime.now(clock)
        );
        return toResponse(documentId, normalizedReaction.name(), changed);
    }

    private FrontContentReactionResponse toResponse(long documentId, String selectedReaction, boolean changed) {
        ContentReactionSummaryRow summary = reactionRepository.getSummary(documentId);
        long helpfulCount = Math.max(0, summary.helpfulCount());
        long notHelpfulCount = Math.max(0, summary.notHelpfulCount());
        long totalCount = helpfulCount + notHelpfulCount;
        int helpfulRate = totalCount == 0
                ? 0
                : (int) Math.round(helpfulCount * 100.0 / totalCount);
        return new FrontContentReactionResponse(
                helpfulCount,
                notHelpfulCount,
                totalCount,
                helpfulRate,
                selectedReaction,
                changed
        );
    }

    private void assertPublicDocument(long documentId) {
        if (documentId <= 0 || !documentRepository.existsPublicDocument(documentId)) {
            throw new FrontContentNotFoundException();
        }
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

    private FrontContentReaction.ReactionType normalizeReaction(String reaction) {
        if (reaction == null || reaction.isBlank()) {
            throw new IllegalArgumentException("콘텐츠 반응이 필요합니다.");
        }
        try {
            return FrontContentReaction.ReactionType.valueOf(reaction.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("지원하지 않는 콘텐츠 반응입니다.");
        }
    }
}
