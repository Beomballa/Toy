package com.section.front.content.repository;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.dto.DocumentListSort;
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
