package com.section.front.content.repository;

import com.section.common.base.entity.type.YN;
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

    private Document document(String title, YN publicYn) {
        Document document = new Document();
        document.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.PUBLISHED,
                publicYn,
                YN.N,
                title,
                title + " 본문",
                null
        );
        return document;
    }
}
