package com.section.front.content.repository;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.dto.DocumentListSort;
import com.section.common.content.dto.PublicDocumentNavigationRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.front.FrontToyApplication;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FrontToyApplication.class)
@ActiveProfiles("local")
@Transactional
class FrontContentSearchRepositoryIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("공개 콘텐츠 정렬은 고정 문서를 우선하고 최신·인기·오래된 순서를 안정적으로 적용한다")
    void sortsPublicContentsWithStableTieBreakers() {
        Document oldest = document("정렬검증 오래된 글", 5, YN.N);
        Document middle = document("정렬검증 중간 글", 20, YN.N);
        Document latest = document("정렬검증 최신 글", 50, YN.N);
        Document pinned = document("정렬검증 고정 글", 1, YN.Y);
        documentRepository.saveAndFlush(oldest);
        documentRepository.saveAndFlush(middle);
        documentRepository.saveAndFlush(latest);
        documentRepository.saveAndFlush(pinned);
        entityManager.clear();

        Page<DocumentListItemDto> latestResult = search(DocumentListSort.LATEST);
        Page<DocumentListItemDto> popularResult = search(DocumentListSort.POPULAR);
        Page<DocumentListItemDto> oldestResult = search(DocumentListSort.OLDEST);

        assertThat(titles(latestResult))
                .containsExactly("정렬검증 고정 글", "정렬검증 최신 글", "정렬검증 중간 글", "정렬검증 오래된 글");
        assertThat(titles(popularResult))
                .containsExactly("정렬검증 고정 글", "정렬검증 최신 글", "정렬검증 중간 글", "정렬검증 오래된 글");
        assertThat(titles(oldestResult))
                .containsExactly("정렬검증 고정 글", "정렬검증 오래된 글", "정렬검증 중간 글", "정렬검증 최신 글");
    }

    @Test
    @DisplayName("인접 콘텐츠는 동일 게시판의 공개 게시글만 작성일과 ID 순서로 조회한다")
    void findsAdjacentPublishedPublicContentsInSameBoard() {
        LocalDateTime olderCreatedAt = LocalDateTime.of(2099, 7, 20, 9, 0);
        LocalDateTime currentCreatedAt = LocalDateTime.of(2099, 7, 21, 9, 0);
        LocalDateTime newerCreatedAt = LocalDateTime.of(2099, 7, 22, 9, 0);
        Document older = document("인접검증 이전 글", 1, YN.N);
        Document current = document("인접검증 현재 글", 1, YN.N);
        Document newer = document("인접검증 다음 글", 1, YN.N);
        Document privateDocument = document("인접검증 비공개 글", 1, YN.N);
        privateDocument.setPublicYn(YN.N);
        Document otherBoard = document("인접검증 다른 게시판 글", 1, YN.N);
        otherBoard.setBoardType(Document.BoardType.STYLE);
        documentRepository.saveAll(List.of(older, current, newer, privateDocument, otherBoard));
        entityManager.flush();
        updateCreatedAt(older, olderCreatedAt);
        updateCreatedAt(current, currentCreatedAt);
        updateCreatedAt(newer, newerCreatedAt);
        updateCreatedAt(privateDocument, LocalDateTime.of(2099, 7, 23, 9, 0));
        updateCreatedAt(otherBoard, LocalDateTime.of(2099, 7, 24, 9, 0));
        entityManager.clear();

        PublicDocumentNavigationRow newerResult = documentRepository.getNewerPublicDocument(
                Document.BoardType.NOTICE,
                currentCreatedAt,
                current.getId()
        ).orElseThrow();
        PublicDocumentNavigationRow olderResult = documentRepository.getOlderPublicDocument(
                Document.BoardType.NOTICE,
                currentCreatedAt,
                current.getId()
        ).orElseThrow();

        assertThat(newerResult.title()).isEqualTo("인접검증 다음 글");
        assertThat(olderResult.title()).isEqualTo("인접검증 이전 글");
    }

    private void updateCreatedAt(Document document, LocalDateTime createdAt) {
        entityManager.createNativeQuery("UPDATE ct_document SET crt_dtm = :createdAt WHERE no = :documentId")
                .setParameter("createdAt", createdAt)
                .setParameter("documentId", document.getId())
                .executeUpdate();
    }

    private Page<DocumentListItemDto> search(DocumentListSort sort) {
        return documentRepository.getDocumentList(
                new DocumentListQuery(
                        null,
                        "정렬검증",
                        Document.PublishStatus.PUBLISHED,
                        YN.Y,
                        false,
                        null,
                        null,
                        null,
                        null,
                        sort
                ),
                PageRequest.of(0, 10)
        );
    }

    private List<String> titles(Page<DocumentListItemDto> result) {
        return result.getContent().stream().map(DocumentListItemDto::getTitle).toList();
    }

    private Document document(String title, int viewCount, YN pinnedYn) {
        Document document = new Document();
        document.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                pinnedYn,
                title,
                title + " 본문",
                null
        );
        document.setViewCnt(viewCount);
        return document;
    }
}
