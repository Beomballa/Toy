package com.section.front.content.service;

import com.section.common.content.dto.ContentReactionSummaryRow;
import com.section.common.content.entity.FrontContentReaction;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.content.repository.FrontContentReactionRepository;
import com.section.front.content.dto.FrontContentReactionResponse;
import com.section.front.content.exception.FrontContentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FrontContentReactionServiceTest {

    private static final long DOCUMENT_ID = 10L;
    private static final String VISITOR_KEY = "123e4567-e89b-12d3-a456-426614174000";
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-24T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private DocumentRepository documentRepository;
    private FrontContentReactionRepository reactionRepository;
    private FrontContentReactionService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        reactionRepository = mock(FrontContentReactionRepository.class);
        service = new FrontContentReactionService(documentRepository, reactionRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("신규 반응은 문서와 방문자 조합으로 저장하고 최신 집계를 반환한다")
    void createsReactionAndReturnsSummary() {
        when(documentRepository.existsPublicDocument(DOCUMENT_ID)).thenReturn(true);
        when(reactionRepository.findByDocumentNoAndVisitorKey(DOCUMENT_ID, VISITOR_KEY))
                .thenReturn(Optional.empty());
        when(reactionRepository.getSummary(DOCUMENT_ID)).thenReturn(new ContentReactionSummaryRow(3, 1));

        FrontContentReactionResponse response = service.react(DOCUMENT_ID, VISITOR_KEY, "helpful");

        assertThat(response.helpfulCount()).isEqualTo(3);
        assertThat(response.totalCount()).isEqualTo(4);
        assertThat(response.helpfulRate()).isEqualTo(75);
        assertThat(response.selectedReaction()).isEqualTo("HELPFUL");
        assertThat(response.changed()).isTrue();
        verify(reactionRepository).upsert(
                eq(DOCUMENT_ID),
                eq(VISITOR_KEY),
                eq("HELPFUL"),
                eq(LocalDateTime.of(2026, 7, 24, 12, 0))
        );
    }

    @Test
    @DisplayName("이미 선택한 반응을 다시 전송하면 집계는 유지하고 변경 없음으로 응답한다")
    void keepsSameReactionIdempotent() {
        FrontContentReaction previous = mock(FrontContentReaction.class);
        when(previous.getReactionType()).thenReturn(FrontContentReaction.ReactionType.NOT_HELPFUL);
        when(documentRepository.existsPublicDocument(DOCUMENT_ID)).thenReturn(true);
        when(reactionRepository.findByDocumentNoAndVisitorKey(DOCUMENT_ID, VISITOR_KEY))
                .thenReturn(Optional.of(previous));
        when(reactionRepository.getSummary(DOCUMENT_ID)).thenReturn(new ContentReactionSummaryRow(0, 1));

        FrontContentReactionResponse response = service.react(DOCUMENT_ID, VISITOR_KEY, "NOT_HELPFUL");

        assertThat(response.changed()).isFalse();
        assertThat(response.helpfulRate()).isZero();
        assertThat(response.selectedReaction()).isEqualTo("NOT_HELPFUL");
    }

    @Test
    @DisplayName("집계 조회는 현재 방문자의 선택과 전체 반응 비율을 함께 반환한다")
    void returnsSummaryWithVisitorSelection() {
        FrontContentReaction previous = mock(FrontContentReaction.class);
        when(previous.getReactionType()).thenReturn(FrontContentReaction.ReactionType.HELPFUL);
        when(documentRepository.existsPublicDocument(DOCUMENT_ID)).thenReturn(true);
        when(reactionRepository.findByDocumentNoAndVisitorKey(DOCUMENT_ID, VISITOR_KEY))
                .thenReturn(Optional.of(previous));
        when(reactionRepository.getSummary(DOCUMENT_ID)).thenReturn(new ContentReactionSummaryRow(2, 1));

        FrontContentReactionResponse response = service.getSummary(DOCUMENT_ID, VISITOR_KEY);

        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(response.helpfulRate()).isEqualTo(67);
        assertThat(response.selectedReaction()).isEqualTo("HELPFUL");
        assertThat(response.changed()).isFalse();
        verify(reactionRepository, never()).upsert(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("비공개 또는 없는 콘텐츠에는 반응을 저장하지 않는다")
    void rejectsUnavailableContent() {
        when(documentRepository.existsPublicDocument(DOCUMENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.react(DOCUMENT_ID, VISITOR_KEY, "HELPFUL"))
                .isInstanceOf(FrontContentNotFoundException.class);
        verify(reactionRepository, never()).upsert(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("지원하지 않는 반응과 잘못된 방문자 키는 저장 전에 거부한다")
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> service.react(DOCUMENT_ID, VISITOR_KEY, "LIKE"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getSummary(DOCUMENT_ID, "short"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(documentRepository, reactionRepository);
    }
}
