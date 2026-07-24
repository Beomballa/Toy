package com.section.front.content.repository;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.ContentReactionSummaryRow;
import com.section.common.content.entity.Document;
import com.section.common.content.entity.FrontContentReaction;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.content.repository.FrontContentReactionRepository;
import com.section.common.content.service.DocumentService;
import com.section.front.FrontToyApplication;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FrontToyApplication.class)
@ActiveProfiles("local")
@Transactional
class FrontContentReactionRepositoryIntegrationTest {

    private static final String VISITOR_KEY = "integration-reaction-visitor-20260724";

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FrontContentReactionRepository reactionRepository;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("반응 upsert는 문서와 방문자별 한 건을 유지하면서 선택을 변경한다")
    void upsertsSingleReactionPerVisitor() {
        Document document = documentRepository.save(publicDocument("반응 집계 공개글"));
        entityManager.flush();
        assertThat(documentRepository.existsPublicDocument(document.getId())).isTrue();
        LocalDateTime reactedAt = LocalDateTime.of(2099, 7, 24, 12, 0);

        reactionRepository.upsert(document.getId(), VISITOR_KEY, "HELPFUL", reactedAt);
        reactionRepository.upsert(document.getId(), VISITOR_KEY, "NOT_HELPFUL", reactedAt.plusMinutes(1));
        entityManager.flush();
        entityManager.clear();

        FrontContentReaction reaction = reactionRepository
                .findByDocumentNoAndVisitorKey(document.getId(), VISITOR_KEY)
                .orElseThrow();
        ContentReactionSummaryRow summary = reactionRepository.getSummary(document.getId());

        assertThat(reaction.getReactionType()).isEqualTo(FrontContentReaction.ReactionType.NOT_HELPFUL);
        assertThat(summary.helpfulCount()).isZero();
        assertThat(summary.notHelpfulCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("콘텐츠 삭제 트랜잭션은 사용자 반응을 먼저 정리한다")
    void deletesReactionsBeforeDocument() {
        Document document = documentRepository.save(publicDocument("반응 삭제 공개글"));
        entityManager.flush();
        reactionRepository.upsert(
                document.getId(),
                VISITOR_KEY,
                "HELPFUL",
                LocalDateTime.of(2099, 7, 24, 12, 0)
        );
        entityManager.flush();

        documentService.deleteDocument(document.getId());
        entityManager.flush();

        assertThat(reactionRepository.findByDocumentNoAndVisitorKey(document.getId(), VISITOR_KEY)).isEmpty();
        assertThat(documentRepository.findById(document.getId())).isEmpty();
    }

    private Document publicDocument(String title) {
        Document document = new Document();
        document.applyEditorValues(
                Document.BoardType.NOTICE,
                Document.PublishStatus.PUBLISHED,
                YN.Y,
                YN.N,
                title,
                title + " 본문",
                null
        );
        return document;
    }
}
