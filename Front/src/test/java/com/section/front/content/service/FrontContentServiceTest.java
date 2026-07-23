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
import com.section.front.content.dto.FrontContentPageResponse;
import com.section.front.content.req.FrontContentListRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FrontContentServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 23);

    private DocumentRepository documentRepository;
    private FrontContentService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        service = new FrontContentService(
                documentRepository,
                Clock.fixed(Instant.parse("2026-07-23T03:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
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
        assertThat(response.popular()).isEmpty();
        assertThat(response.popularStartDate()).isEqualTo("2026-07-17");
        assertThat(response.popularEndDate()).isEqualTo("2026-07-23");
    }

    @Test
    @DisplayName("콘텐츠 제한값과 최근 7일 기간은 QueryDSL 조회에 동일하게 전달한다")
    void passesLimitAndDateRangeToQueries() {
        when(documentRepository.getPublicDocuments(Document.BoardType.NOTICE, 6)).thenReturn(List.of());
        when(documentRepository.getPublicDocuments(Document.BoardType.STYLE, 6)).thenReturn(List.of());

        service.getHighlights(6);

        verify(documentRepository).getPublicDocuments(Document.BoardType.NOTICE, 6);
        verify(documentRepository).getPublicDocuments(Document.BoardType.STYLE, 6);
        verify(documentRepository).getPopularPublicDocuments(TODAY.minusDays(6), TODAY, 6);
    }

    @Test
    @DisplayName("최근 조회 이벤트 기반 인기 콘텐츠 지표를 안전한 요약 형태로 반환한다")
    void returnsPopularContentsFromRecentViewEvents() {
        when(documentRepository.getPopularPublicDocuments(TODAY.minusDays(6), TODAY, 4)).thenReturn(List.of(
                new PopularPublicContentRow(
                        21L,
                        Document.BoardType.STYLE,
                        "여름 레이어링",
                        "<p>가볍게 <strong>겹쳐 입는</strong> 방법</p>",
                        18,
                        12,
                        YN.Y,
                        LocalDateTime.of(2026, 7, 21, 9, 0)
                )
        ));

        FrontContentHighlightsResponse response = service.getHighlights(4);

        assertThat(response.popular()).singleElement().satisfies(item -> {
            assertThat(item.boardType()).isEqualTo("STYLE");
            assertThat(item.summary()).isEqualTo("가볍게 겹쳐 입는 방법");
            assertThat(item.recentViewCount()).isEqualTo(18);
            assertThat(item.uniqueVisitors()).isEqualTo(12);
            assertThat(item.pinned()).isTrue();
            assertThat(item.createdDate()).isEqualTo("2026-07-21");
        });
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
        when(documentRepository.getNewerPublicDocument(
                Document.BoardType.STYLE,
                current.createdAt(),
                current.id()
        )).thenReturn(Optional.of(new PublicDocumentNavigationRow(
                12L,
                Document.BoardType.STYLE,
                "새로운 스타일",
                LocalDateTime.of(2026, 7, 23, 9, 0)
        )));
        when(documentRepository.getOlderPublicDocument(
                Document.BoardType.STYLE,
                current.createdAt(),
                current.id()
        )).thenReturn(Optional.of(new PublicDocumentNavigationRow(
                9L,
                Document.BoardType.STYLE,
                "이전 스타일",
                LocalDateTime.of(2026, 7, 21, 9, 0)
        )));

        FrontContentDetailResponse response = service.findDetail(10L).orElseThrow();

        assertThat(response.content()).isEqualTo("가벼운 여름 스타일");
        assertThat(response.boardType()).isEqualTo("STYLE");
        assertThat(response.estimatedReadMinutes()).isEqualTo(1);
        assertThat(response.characterCount()).isEqualTo(10);
        assertThat(response.newerContent().title()).isEqualTo("새로운 스타일");
        assertThat(response.olderContent().title()).isEqualTo("이전 스타일");
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

    @Test
    @DisplayName("공개 콘텐츠 검색은 게시 완료·공개 조건을 강제하고 페이징 메타를 반환한다")
    void searchesOnlyPublishedPublicContents() {
        DocumentListItemDto item = new DocumentListItemDto();
        item.setId(31L);
        item.setBoardType("NOTICE");
        item.setTitle("운영 공지");
        item.setContentPreview("<p>중요한 안내</p>");
        item.setViewCnt(20);
        item.setPinnedYn("Y");
        item.setCrtDtm(LocalDateTime.of(2026, 7, 22, 10, 0));
        when(documentRepository.getDocumentList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 8), 9));

        FrontContentPageResponse response =
                service.search(new FrontContentListRequest("NOTICE", " 운영 ", 0, 8, "POPULAR"));

        ArgumentCaptor<DocumentListQuery> queryCaptor = ArgumentCaptor.forClass(DocumentListQuery.class);
        verify(documentRepository).getDocumentList(queryCaptor.capture(), org.mockito.ArgumentMatchers.any());
        assertThat(queryCaptor.getValue().boardType()).isEqualTo(Document.BoardType.NOTICE);
        assertThat(queryCaptor.getValue().keyword()).isEqualTo("운영");
        assertThat(queryCaptor.getValue().status()).isEqualTo(Document.PublishStatus.PUBLISHED);
        assertThat(queryCaptor.getValue().publicYn()).isEqualTo(YN.Y);
        assertThat(queryCaptor.getValue().sort()).isEqualTo(DocumentListSort.POPULAR);
        assertThat(response.totalElements()).isEqualTo(9);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.sort()).isEqualTo("POPULAR");
        assertThat(response.pageViewCount()).isEqualTo(20);
        assertThat(response.pagePinnedCount()).isEqualTo(1);
        assertThat(response.pageNoticeCount()).isEqualTo(1);
        assertThat(response.pageStyleCount()).isZero();
        assertThat(response.items()).singleElement().satisfies(content -> {
            assertThat(content.summary()).isEqualTo("중요한 안내");
            assertThat(content.pinned()).isTrue();
        });
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
