package com.section.front.content.service;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.PublicDocumentRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.front.content.dto.FrontContentHighlightsResponse;
import com.section.front.content.dto.FrontContentDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FrontContentServiceTest {

    private DocumentRepository documentRepository;
    private FrontContentService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        service = new FrontContentService(documentRepository);
    }

    @Test
    @DisplayName("공지와 스타일 공개 콘텐츠를 함께 반환하고 HTML 본문을 요약한다")
    void returnsNoticeAndStyleHighlights() {
        when(documentRepository.getPublicDocuments(Document.BoardType.NOTICE, 4)).thenReturn(List.of(
                row(1L, Document.BoardType.NOTICE, "배송 공지", "<p>배송 <strong>일정</strong> 안내</p>", YN.Y)
        ));
        when(documentRepository.getPublicDocuments(Document.BoardType.STYLE, 4)).thenReturn(List.of(
                row(2L, Document.BoardType.STYLE, "여름 스타일", "가벼운 스타일 제안", YN.N)
        ));

        FrontContentHighlightsResponse response = service.getHighlights(null);

        assertThat(response.notices()).singleElement().satisfies(item -> {
            assertThat(item.title()).isEqualTo("배송 공지");
            assertThat(item.summary()).isEqualTo("배송 일정 안내");
            assertThat(item.pinned()).isTrue();
            assertThat(item.createdDate()).isEqualTo("2026-07-22");
        });
        assertThat(response.styles()).singleElement().satisfies(item ->
                assertThat(item.title()).isEqualTo("여름 스타일")
        );
    }

    @Test
    @DisplayName("콘텐츠 제한값은 QueryDSL 조회에 동일하게 전달한다")
    void passesLimitToBothQueries() {
        when(documentRepository.getPublicDocuments(Document.BoardType.NOTICE, 6)).thenReturn(List.of());
        when(documentRepository.getPublicDocuments(Document.BoardType.STYLE, 6)).thenReturn(List.of());

        service.getHighlights(6);

        verify(documentRepository).getPublicDocuments(Document.BoardType.NOTICE, 6);
        verify(documentRepository).getPublicDocuments(Document.BoardType.STYLE, 6);
    }

    @Test
    @DisplayName("허용 범위를 벗어난 노출 개수는 거부한다")
    void rejectsInvalidLimit() {
        assertThatThrownBy(() -> service.getHighlights(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getHighlights(9)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("공개 콘텐츠 상세는 HTML을 제거하고 현재 글을 제외한 연관 콘텐츠를 반환한다")
    void returnsPlainDetailWithRelatedContents() {
        PublicDocumentRow current = row(10L, Document.BoardType.STYLE, "레이어링 제안", "<p>가벼운 <b>여름</b> 스타일</p>", YN.Y);
        PublicDocumentRow related = row(11L, Document.BoardType.STYLE, "스니커즈 제안", "데일리 슈즈", YN.N);
        when(documentRepository.getPublicDocument(10L)).thenReturn(Optional.of(current));
        when(documentRepository.getPublicDocuments(Document.BoardType.STYLE, 5)).thenReturn(List.of(current, related));

        FrontContentDetailResponse response = service.findDetail(10L).orElseThrow();

        assertThat(response.content()).isEqualTo("가벼운 여름 스타일");
        assertThat(response.boardType()).isEqualTo("STYLE");
        assertThat(response.relatedContents()).singleElement().satisfies(item ->
                assertThat(item.id()).isEqualTo(11L)
        );
    }

    @Test
    @DisplayName("공개 콘텐츠가 없으면 상세 결과도 비어 있다")
    void returnsEmptyWhenDetailIsNotPublic() {
        when(documentRepository.getPublicDocument(999L)).thenReturn(Optional.empty());

        assertThat(service.findDetail(999L)).isEmpty();
    }

    private PublicDocumentRow row(
            long id,
            Document.BoardType boardType,
            String title,
            String content,
            YN pinnedYn
    ) {
        return new PublicDocumentRow(
                id, boardType, title, content, 12, pinnedYn, LocalDateTime.of(2026, 7, 22, 9, 0)
        );
    }
}
