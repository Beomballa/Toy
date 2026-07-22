package com.section.admin.content.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.dto.DocumentSummaryDto;
import com.section.common.content.dto.PublicDocumentRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class DocumentRepositorySearchIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("프론트 공개 콘텐츠는 게시 완료된 공개 문서만 고정글 우선으로 조회한다")
    void getPublicDocumentsExcludesDraftAndPrivateDocuments() {
        Document publishedPublic = document("프론트 공개 공지", Document.PublishStatus.PUBLISHED, YN.Y, YN.Y);
        Document publishedPublicNormal = document("프론트 일반 공지", Document.PublishStatus.PUBLISHED, YN.Y, YN.N);
        Document publishedPrivate = document("프론트 비공개 공지", Document.PublishStatus.PUBLISHED, YN.N, YN.N);
        Document draftPublic = document("프론트 임시 공지", Document.PublishStatus.DRAFT, YN.Y, YN.N);
        documentRepository.saveAll(List.of(publishedPublic, publishedPublicNormal, publishedPrivate, draftPublic));
        entityManager.flush();
        entityManager.clear();

        List<PublicDocumentRow> result = documentRepository.getPublicDocuments(Document.BoardType.NOTICE, 8);

        assertTrue(result.stream().anyMatch(row -> row.title().equals("프론트 공개 공지")));
        assertFalse(result.stream().anyMatch(row -> row.title().equals("프론트 비공개 공지")));
        assertFalse(result.stream().anyMatch(row -> row.title().equals("프론트 임시 공지")));
        int pinnedIndex = indexOfTitle(result, "프론트 공개 공지");
        int normalIndex = indexOfTitle(result, "프론트 일반 공지");
        assertTrue(pinnedIndex >= 0 && normalIndex >= 0 && pinnedIndex < normalIndex);
        assertTrue(documentRepository.getPublicDocument(publishedPublic.getId()).isPresent());
        assertTrue(documentRepository.getPublicDocument(publishedPrivate.getId()).isEmpty());
        assertTrue(documentRepository.getPublicDocument(draftPublic.getId()).isEmpty());
    }

    private Document document(String title, Document.PublishStatus status, YN publicYn, YN pinnedYn) {
        Document document = new Document();
        document.applyEditorValues(
                Document.BoardType.NOTICE,
                status,
                publicYn,
                pinnedYn,
                title,
                title + " 본문",
                null
        );
        return document;
    }

    private int indexOfTitle(List<PublicDocumentRow> rows, String title) {
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).title().equals(title)) {
                return index;
            }
        }
        return -1;
    }

    @Test
    @DisplayName("콘텐츠 목록은 고정글 우선 정렬과 상태/공개 필터를 함께 적용한다")
    void getDocumentListAppliesPinnedSortingAndVisibilityFilters() {
        Document pinnedNotice = new Document();
        pinnedNotice.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                YN.Y,
                "고정 공지",
                "고정 공지 본문",
                null
        );
        documentRepository.save(pinnedNotice);

        Document hiddenNotice = new Document();
        hiddenNotice.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.PUBLISHED,
                YN.N,
                YN.N,
                "비공개 공지",
                "비공개 본문",
                null
        );
        documentRepository.save(hiddenNotice);

        Document draftNotice = new Document();
        draftNotice.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.DRAFT,
                YN.Y,
                YN.N,
                "임시 공지",
                "임시 본문",
                null
        );
        documentRepository.save(draftNotice);

        Page<DocumentListItemDto> pinnedOnly = documentRepository.getDocumentList(
                new DocumentListQuery(Document.BoardType.NOTICE, "공지", Document.PublishStatus.PUBLISHED, YN.Y, true, null, null, null, null),
                PageRequest.of(0, 10)
        );

        assertEquals(1, pinnedOnly.getTotalElements());
        assertEquals("고정 공지", pinnedOnly.getContent().getFirst().getTitle());
        assertEquals("Y", pinnedOnly.getContent().getFirst().getPinnedYn());
    }

    @Test
    @DisplayName("콘텐츠 목록은 생성일 범위 필터를 함께 적용한다")
    void getDocumentListAppliesCreatedDateRange() {
        Document recentNotice = new Document();
        recentNotice.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                YN.N,
                "이번 주 공지",
                "이번 주 본문",
                null
        );
        documentRepository.save(recentNotice);
        entityManager.createNativeQuery("update CT_DOCUMENT set crt_dtm = :crtDtm where NO = :id")
                .setParameter("crtDtm", java.time.LocalDateTime.of(2026, 5, 20, 12, 0))
                .setParameter("id", recentNotice.getId())
                .executeUpdate();

        Document oldNotice = new Document();
        oldNotice.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                YN.N,
                "지난달 공지",
                "지난달 본문",
                null
        );
        documentRepository.save(oldNotice);
        entityManager.createNativeQuery("update CT_DOCUMENT set crt_dtm = :crtDtm where NO = :id")
                .setParameter("crtDtm", java.time.LocalDateTime.of(2026, 4, 10, 12, 0))
                .setParameter("id", oldNotice.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        Page<DocumentListItemDto> result = documentRepository.getDocumentList(
                new DocumentListQuery(
                        Document.BoardType.NOTICE,
                        null,
                        Document.PublishStatus.PUBLISHED,
                        YN.Y,
                        false,
                        null,
                        null,
                        java.time.LocalDateTime.of(2026, 5, 1, 0, 0),
                        java.time.LocalDateTime.of(2026, 5, 31, 23, 59, 59)
                ),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("이번 주 공지", result.getContent().getFirst().getTitle());
    }

    @Test
    @DisplayName("콘텐츠 목록 키워드 검색은 다중 검색어를 모두 만족하는 문서만 조회한다")
    void getDocumentListMatchesAllKeywordTerms() {
        Document matchedDocument = new Document();
        matchedDocument.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                YN.N,
                "여름 배송 공지",
                "오늘 배송 일정과 공지 내용을 안내합니다.",
                null
        );
        documentRepository.save(matchedDocument);

        Document partialDocument = new Document();
        partialDocument.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                YN.N,
                "여름 이벤트",
                "공지와 무관한 이벤트 소식입니다.",
                null
        );
        documentRepository.save(partialDocument);
        entityManager.flush();
        entityManager.clear();

        Page<DocumentListItemDto> result = documentRepository.getDocumentList(
                new DocumentListQuery(Document.BoardType.NOTICE, "여름 배송", Document.PublishStatus.PUBLISHED, YN.Y, false, null, null, null, null),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("여름 배송 공지", result.getContent().getFirst().getTitle());
    }

    @Test
    @DisplayName("콘텐츠 목록은 상품 연결 여부와 상품번호 필터를 함께 적용한다")
    void getDocumentListAppliesProductLinkFilters() {
        Document linkedStyle = new Document();
        linkedStyle.applyEditorValues(
                Document.BoardType.STYLE,
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                YN.N,
                "자동연결스타일 연결글",
                "자동연결스타일 키워드를 가진 상품 연결 게시글입니다.",
                1001L
        );
        documentRepository.save(linkedStyle);

        Document unlinkedStyle = new Document();
        unlinkedStyle.applyEditorValues(
                Document.BoardType.STYLE,
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                YN.N,
                "자동연결스타일 일반글",
                "자동연결스타일 키워드를 가진 상품 미연결 게시글입니다.",
                null
        );
        documentRepository.save(unlinkedStyle);
        entityManager.flush();
        entityManager.clear();

        Page<DocumentListItemDto> linkedResult = documentRepository.getDocumentList(
                new DocumentListQuery(Document.BoardType.STYLE, "자동연결스타일", Document.PublishStatus.PUBLISHED, YN.Y, false, 1001L, true, null, null),
                PageRequest.of(0, 10)
        );
        Page<DocumentListItemDto> unlinkedResult = documentRepository.getDocumentList(
                new DocumentListQuery(Document.BoardType.STYLE, "자동연결스타일", Document.PublishStatus.PUBLISHED, YN.Y, false, null, false, null, null),
                PageRequest.of(0, 10)
        );

        assertEquals(1, linkedResult.getTotalElements());
        assertEquals("자동연결스타일 연결글", linkedResult.getContent().getFirst().getTitle());
        assertEquals(1001L, linkedResult.getContent().getFirst().getProductNo());
        assertEquals(1, unlinkedResult.getTotalElements());
        assertEquals("자동연결스타일 일반글", unlinkedResult.getContent().getFirst().getTitle());
    }

    @Test
    @DisplayName("콘텐츠 요약은 현재 검색 조건 기준 상태와 조회수 집계를 반환한다")
    void getDocumentSummaryReturnsAggregatedCounts() {
        Document publishedLinked = new Document();
        publishedLinked.applyEditorValues(
                Document.BoardType.QNA,
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                YN.Y,
                "자동요약QNA 공개글",
                "자동요약QNA 키워드를 가진 공개 템플릿입니다.",
                501L
        );
        publishedLinked.setViewCnt(12);
        documentRepository.save(publishedLinked);

        Document draftUnlinked = new Document();
        draftUnlinked.applyEditorValues(
                Document.BoardType.QNA,
                Document.PublishStatus.DRAFT,
                YN.N,
                YN.N,
                "자동요약QNA 비공개글",
                "자동요약QNA 키워드를 가진 비공개 문의입니다.",
                null
        );
        draftUnlinked.setViewCnt(4);
        documentRepository.save(draftUnlinked);
        entityManager.flush();
        entityManager.clear();

        DocumentSummaryDto summary = documentRepository.getDocumentSummary(
                new DocumentListQuery(Document.BoardType.QNA, "자동요약QNA", null, null, false, null, null, null, null)
        );

        assertEquals(2, summary.totalCount());
        assertEquals(1, summary.publishedCount());
        assertEquals(1, summary.draftCount());
        assertEquals(1, summary.publicCount());
        assertEquals(1, summary.privateCount());
        assertEquals(1, summary.pinnedCount());
        assertEquals(1, summary.linkedCount());
        assertEquals(16, summary.totalViewCount());
    }
}
