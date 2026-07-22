package com.section.front.content.service;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.PublicDocumentRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.content.repository.FrontContentViewEventRepository;
import com.section.front.content.dto.FrontContentViewResponse;
import com.section.front.content.exception.FrontContentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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

class FrontContentViewServiceTest {

    private static final String VISITOR_KEY = "123e4567-e89b-12d3-a456-426614174000";

    private DocumentRepository documentRepository;
    private FrontContentViewEventRepository viewEventRepository;
    private FrontContentViewService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        viewEventRepository = mock(FrontContentViewEventRepository.class);
        service = new FrontContentViewService(documentRepository, viewEventRepository);
    }

    @Test
    @DisplayName("당일 첫 조회는 이벤트와 공개 문서 조회수를 함께 증가시킨다")
    void recordsFirstDailyView() {
        when(documentRepository.getPublicDocument(10L)).thenReturn(Optional.of(row(12)));
        when(viewEventRepository.insertIfAbsent(eq(10L), eq(VISITOR_KEY), any(), any())).thenReturn(1);
        when(documentRepository.incrementPublicViewCount(10L, Document.PublishStatus.PUBLISHED, YN.Y)).thenReturn(1);

        FrontContentViewResponse response = service.record(10L, VISITOR_KEY);

        assertThat(response.counted()).isTrue();
        assertThat(response.viewCount()).isEqualTo(13);
        verify(documentRepository).incrementPublicViewCount(10L, Document.PublishStatus.PUBLISHED, YN.Y);
    }

    @Test
    @DisplayName("같은 방문자의 당일 중복 조회는 조회수를 다시 증가시키지 않는다")
    void ignoresDuplicateDailyView() {
        when(documentRepository.getPublicDocument(10L)).thenReturn(Optional.of(row(12)));
        when(viewEventRepository.insertIfAbsent(eq(10L), eq(VISITOR_KEY), any(), any())).thenReturn(0);

        FrontContentViewResponse response = service.record(10L, VISITOR_KEY);

        assertThat(response.counted()).isFalse();
        assertThat(response.viewCount()).isEqualTo(12);
        verify(documentRepository, never()).incrementPublicViewCount(anyLong(), any(), any());
    }

    @Test
    @DisplayName("비공개 또는 없는 콘텐츠는 조회 이벤트를 남기지 않는다")
    void rejectsUnavailableContent() {
        when(documentRepository.getPublicDocument(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.record(999L, VISITOR_KEY))
                .isInstanceOf(FrontContentNotFoundException.class);
        verifyNoInteractions(viewEventRepository);
    }

    @Test
    @DisplayName("형식이 잘못된 방문자 키는 저장 전에 거부한다")
    void rejectsInvalidVisitorKey() {
        assertThatThrownBy(() -> service.record(10L, "short"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(documentRepository, viewEventRepository);
    }

    private PublicDocumentRow row(int viewCount) {
        return new PublicDocumentRow(
                10L,
                Document.BoardType.NOTICE,
                "공지",
                "본문",
                viewCount,
                YN.N,
                LocalDateTime.of(2026, 7, 22, 10, 0)
        );
    }
}
