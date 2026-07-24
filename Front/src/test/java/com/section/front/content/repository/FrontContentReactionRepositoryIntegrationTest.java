package com.section.front.content.repository;

import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.ContentReactionAnalyticsSummaryRow;
import com.section.common.content.dto.ContentReactionDataQualityRow;
import com.section.common.content.dto.ContentReactionSummaryRow;
import com.section.common.content.dto.ContentReactionTopRow;
import com.section.common.content.dto.ContentReactionTrendRow;
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
import java.util.List;

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

    @Test
    @DisplayName("반응 분석 조회는 기간과 게시판별 요약, 일별 추이, 상위 콘텐츠를 함께 집계한다")
    void aggregatesReactionAnalyticsWithDynamicFilters() {
        Document notice = documentRepository.save(publicDocument("분석 공지"));
        Document style = documentRepository.save(publicDocument("분석 스타일", Document.BoardType.STYLE));
        entityManager.flush();
        LocalDateTime firstDay = LocalDateTime.of(2099, 7, 23, 11, 0);
        LocalDateTime secondDay = LocalDateTime.of(2099, 7, 24, 12, 0);

        reactionRepository.upsert(notice.getId(), VISITOR_KEY + "-1", "HELPFUL", firstDay);
        reactionRepository.upsert(notice.getId(), VISITOR_KEY + "-2", "NOT_HELPFUL", secondDay);
        reactionRepository.upsert(style.getId(), VISITOR_KEY + "-3", "HELPFUL", secondDay);
        entityManager.flush();
        entityManager.clear();

        ContentReactionAnalyticsSummaryRow summary = reactionRepository.getAnalyticsSummary(
                firstDay.toLocalDate().atStartOfDay(),
                secondDay.toLocalDate().plusDays(1).atStartOfDay(),
                Document.BoardType.NOTICE
        );
        List<ContentReactionTrendRow> trend = reactionRepository.getDailyReactionTrend(
                firstDay.toLocalDate().atStartOfDay(),
                secondDay.toLocalDate().plusDays(1).atStartOfDay(),
                Document.BoardType.NOTICE
        );
        List<ContentReactionTopRow> top = reactionRepository.getTopReactedContents(
                firstDay.toLocalDate().atStartOfDay(),
                secondDay.toLocalDate().plusDays(1).atStartOfDay(),
                Document.BoardType.NOTICE,
                10
        );
        List<ContentReactionTrendRow> documentTrend = reactionRepository.getDailyReactionTrend(
                notice.getId(),
                firstDay.toLocalDate().atStartOfDay(),
                secondDay.toLocalDate().plusDays(1).atStartOfDay()
        );

        assertThat(summary).isEqualTo(new ContentReactionAnalyticsSummaryRow(2, 1, 1, 2, 1));
        assertThat(trend).containsExactly(
                new ContentReactionTrendRow(firstDay.toLocalDate(), 1, 0),
                new ContentReactionTrendRow(secondDay.toLocalDate(), 0, 1)
        );
        assertThat(top).singleElement().satisfies(row -> {
            assertThat(row.documentId()).isEqualTo(notice.getId());
            assertThat(row.totalCount()).isEqualTo(2);
            assertThat(row.helpfulCount()).isEqualTo(1);
            assertThat(row.notHelpfulCount()).isEqualTo(1);
        });
        assertThat(documentTrend).containsExactlyElementsOf(trend);
    }

    @Test
    @DisplayName("반응 데이터 품질 조회는 삭제된 문서를 가리키는 고아 반응을 구분한다")
    void detectsOrphanReactions() {
        ContentReactionDataQualityRow before = reactionRepository.getDataQuality();
        reactionRepository.upsert(
                Long.MAX_VALUE,
                VISITOR_KEY + "-orphan",
                "HELPFUL",
                LocalDateTime.of(2099, 7, 24, 12, 0)
        );
        entityManager.flush();
        entityManager.clear();

        ContentReactionDataQualityRow after = reactionRepository.getDataQuality();

        assertThat(after.totalCount()).isEqualTo(before.totalCount() + 1);
        assertThat(after.validCount()).isEqualTo(before.validCount());
        assertThat(after.orphanCount()).isEqualTo(before.orphanCount() + 1);
    }

    private Document publicDocument(String title) {
        return publicDocument(title, Document.BoardType.NOTICE);
    }

    private Document publicDocument(String title, Document.BoardType boardType) {
        Document document = new Document();
        document.applyEditorValues(
                boardType,
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
