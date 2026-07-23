package com.section.front.content.repository;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.PopularPublicContentRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.content.repository.FrontContentViewEventRepository;
import com.section.front.FrontToyApplication;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FrontToyApplication.class)
@ActiveProfiles("local")
@Transactional
class FrontContentViewRepositoryIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FrontContentViewEventRepository viewEventRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("조회 이벤트는 방문자별 일 1회만 저장하고 공개 문서 조회수만 원자 증가한다")
    void deduplicatesDailyViewsAndIncrementsOnlyPublicDocument() {
        Document publicDocument = document("조회 통합 공개글", YN.Y);
        Document privateDocument = document("조회 통합 비공개글", YN.N);
        documentRepository.save(publicDocument);
        documentRepository.save(privateDocument);
        entityManager.flush();

        String visitorKey = "integration-visitor-20260722";
        LocalDate viewedDate = LocalDate.of(2026, 7, 22);
        LocalDateTime viewedDtm = viewedDate.atTime(12, 0);
        int firstInsert = viewEventRepository.insertIfAbsent(publicDocument.getId(), visitorKey, viewedDate, viewedDtm);
        int duplicateInsert = viewEventRepository.insertIfAbsent(publicDocument.getId(), visitorKey, viewedDate, viewedDtm.plusHours(1));
        int publicUpdated = documentRepository.incrementPublicViewCount(
                publicDocument.getId(), Document.PublishStatus.PUBLISHED, YN.Y
        );
        int privateUpdated = documentRepository.incrementPublicViewCount(
                privateDocument.getId(), Document.PublishStatus.PUBLISHED, YN.Y
        );
        entityManager.flush();
        entityManager.clear();

        assertThat(firstInsert).isEqualTo(1);
        assertThat(duplicateInsert).isZero();
        assertThat(publicUpdated).isEqualTo(1);
        assertThat(privateUpdated).isZero();
        assertThat(documentRepository.findById(publicDocument.getId()).orElseThrow().getViewCnt()).isEqualTo(1);
    }

    @Test
    @DisplayName("최근 인기 콘텐츠는 기간 내 공개 게시글만 조회 이벤트 수와 순 방문자 순으로 집계한다")
    void aggregatesPopularPublicContentsWithinDateRange() {
        Document popular = document("최근 인기 공개글", YN.Y);
        Document secondary = document("두 번째 공개글", YN.Y);
        Document privateDocument = document("인기 비공개글", YN.N);
        Document draftDocument = document("인기 게시 전 글", YN.Y, Document.PublishStatus.DRAFT);
        documentRepository.saveAll(List.of(popular, secondary, privateDocument, draftDocument));
        entityManager.flush();

        LocalDate startDate = LocalDate.of(2099, 7, 17);
        LocalDate endDate = LocalDate.of(2099, 7, 23);
        insertView(popular, "popular-visitor-a", endDate, 10);
        insertView(popular, "popular-visitor-b", endDate.minusDays(1), 11);
        insertView(popular, "popular-visitor-a", endDate.minusDays(2), 12);
        insertView(secondary, "secondary-visitor", endDate, 13);
        insertView(privateDocument, "private-visitor-a", endDate, 14);
        insertView(privateDocument, "private-visitor-b", endDate, 15);
        insertView(draftDocument, "draft-visitor-a", endDate, 16);
        insertView(draftDocument, "draft-visitor-b", endDate.minusDays(1), 17);
        insertView(secondary, "expired-visitor", startDate.minusDays(1), 16);
        entityManager.flush();
        entityManager.clear();

        List<PopularPublicContentRow> result =
                documentRepository.getPopularPublicDocuments(startDate, endDate, 4);

        assertThat(result).extracting(PopularPublicContentRow::title)
                .containsExactly("최근 인기 공개글", "두 번째 공개글");
        assertThat(result.get(0).recentViewCount()).isEqualTo(3);
        assertThat(result.get(0).uniqueVisitors()).isEqualTo(2);
        assertThat(result.get(1).recentViewCount()).isEqualTo(1);
        assertThat(result.get(1).uniqueVisitors()).isEqualTo(1);
    }

    private void insertView(Document document, String visitorKey, LocalDate viewedDate, int hour) {
        int inserted = viewEventRepository.insertIfAbsent(
                document.getId(),
                visitorKey,
                viewedDate,
                viewedDate.atTime(hour, 0)
        );
        assertThat(inserted).isEqualTo(1);
    }

    private Document document(String title, YN publicYn) {
        return document(title, publicYn, Document.PublishStatus.PUBLISHED);
    }

    private Document document(String title, YN publicYn, Document.PublishStatus status) {
        Document document = new Document();
        document.applyEditorValues(
                Document.BoardType.NOTICE,
                status,
                publicYn,
                YN.N,
                title,
                title + " 본문",
                null
        );
        return document;
    }
}
