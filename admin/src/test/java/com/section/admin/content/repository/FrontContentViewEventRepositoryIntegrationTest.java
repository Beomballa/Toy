package com.section.admin.content.repository;

import com.section.admin.AdminToyApplication;
import com.section.common.base.entity.type.YN;
import com.section.common.content.dto.ContentViewSummaryRow;
import com.section.common.content.dto.ContentViewDataQualityRow;
import com.section.common.content.dto.ContentViewTopRow;
import com.section.common.content.dto.ContentViewTrendRow;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.content.repository.FrontContentViewEventRepository;
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

@SpringBootTest(classes = AdminToyApplication.class)
@ActiveProfiles("local")
@Transactional
class FrontContentViewEventRepositoryIntegrationTest {

    @Autowired
    private FrontContentViewEventRepository viewEventRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("조회 이벤트 집계는 게시판 필터와 순 방문자 중복 제거를 함께 적용한다")
    void aggregatesViewEventsByBoardAndVisitor() {
        Document notice = document("배송 공지", Document.BoardType.NOTICE);
        Document style = document("여름 스타일", Document.BoardType.STYLE);
        documentRepository.saveAll(List.of(notice, style));
        entityManager.flush();

        insertEvent(notice.getId(), "visitor-a", LocalDate.of(2026, 7, 22), 10);
        insertEvent(notice.getId(), "visitor-b", LocalDate.of(2026, 7, 22), 11);
        insertEvent(notice.getId(), "visitor-a", LocalDate.of(2026, 7, 23), 12);
        insertEvent(style.getId(), "visitor-c", LocalDate.of(2026, 7, 23), 13);
        entityManager.flush();
        entityManager.clear();

        ContentViewSummaryRow summary = viewEventRepository.getViewSummary(
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 23),
                Document.BoardType.NOTICE
        );
        List<ContentViewTrendRow> trend = viewEventRepository.getDailyViewTrend(
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 23),
                Document.BoardType.NOTICE
        );
        List<ContentViewTopRow> topContents = viewEventRepository.getTopViewedContents(
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 23),
                null,
                5
        );

        assertThat(summary.totalViews()).isEqualTo(3);
        assertThat(summary.uniqueVisitors()).isEqualTo(2);
        assertThat(summary.viewedContentCount()).isEqualTo(1);
        assertThat(trend).extracting(ContentViewTrendRow::viewCount).containsExactly(2L, 1L);
        assertThat(topContents).hasSizeGreaterThanOrEqualTo(2);
        assertThat(topContents.getFirst().documentId()).isEqualTo(notice.getId());
        assertThat(topContents.getFirst().viewCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("조회 이벤트 보존 삭제는 기준일 이전 데이터만 일괄 제거한다")
    void deletesOnlyEventsBeforeRetentionStartDate() {
        Document notice = document("보존 정책 공지", Document.BoardType.NOTICE);
        documentRepository.save(notice);
        entityManager.flush();
        insertEvent(notice.getId(), "expired-visitor", LocalDate.of(2026, 6, 23), 10);
        insertEvent(notice.getId(), "boundary-visitor", LocalDate.of(2026, 6, 24), 11);
        insertEvent(notice.getId(), "recent-visitor", LocalDate.of(2026, 7, 23), 12);
        entityManager.flush();
        entityManager.clear();

        int deletedCount = viewEventRepository.deleteBefore(LocalDate.of(2026, 6, 24));

        Number remaining = (Number) entityManager.createNativeQuery("""
                        select count(*)
                        from front_content_view_event
                        where document_no = :documentNo
                        """)
                .setParameter("documentNo", notice.getId())
                .getSingleResult();
        assertThat(deletedCount).isEqualTo(1);
        assertThat(remaining.longValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("전체 조회 분석은 삭제된 문서를 가리키는 고아 이벤트를 제외한다")
    void excludesOrphanEventsFromAllBoardAnalytics() {
        Document notice = document("정상 조회 공지", Document.BoardType.NOTICE);
        documentRepository.save(notice);
        entityManager.flush();
        LocalDate viewedDate = LocalDate.of(2026, 7, 23);
        insertEvent(notice.getId(), "valid-visitor", viewedDate, 10);
        insertEvent(Long.MAX_VALUE, "orphan-visitor", viewedDate, 11);
        entityManager.flush();
        entityManager.clear();

        ContentViewSummaryRow summary = viewEventRepository.getViewSummary(viewedDate, viewedDate, null);
        List<ContentViewTrendRow> trend = viewEventRepository.getDailyViewTrend(viewedDate, viewedDate, null);

        assertThat(summary.totalViews()).isEqualTo(1);
        assertThat(summary.uniqueVisitors()).isEqualTo(1);
        assertThat(summary.viewedContentCount()).isEqualTo(1);
        assertThat(trend).singleElement().satisfies(item -> {
            assertThat(item.viewCount()).isEqualTo(1);
            assertThat(item.uniqueVisitors()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("조회 이벤트 품질 집계와 고아 정리는 같은 문서 존재 기준을 사용한다")
    void measuresAndDeletesOrphanEvents() {
        Document notice = document("품질 점검 공지", Document.BoardType.NOTICE);
        documentRepository.save(notice);
        entityManager.flush();
        LocalDate viewedDate = LocalDate.of(2026, 7, 23);
        insertEvent(notice.getId(), "quality-valid", viewedDate, 10);
        insertEvent(Long.MAX_VALUE - 1, "quality-orphan", viewedDate, 11);
        entityManager.flush();
        entityManager.clear();

        ContentViewDataQualityRow before = viewEventRepository.getDataQuality();
        int deletedCount = viewEventRepository.deleteOrphanEvents();
        Number remainingOrphans = (Number) entityManager.createNativeQuery("""
                        select count(*)
                        from front_content_view_event
                        where document_no = :documentNo
                        """)
                .setParameter("documentNo", Long.MAX_VALUE - 1)
                .getSingleResult();

        assertThat(before.orphanEventCount()).isGreaterThanOrEqualTo(1);
        assertThat(before.validEventCount()).isGreaterThanOrEqualTo(1);
        assertThat(before.oldestViewedDate()).isNotNull();
        assertThat(before.latestViewedDate()).isNotNull();
        assertThat(deletedCount).isGreaterThanOrEqualTo(1);
        assertThat(remainingOrphans.longValue()).isZero();
    }

    private Document document(String title, Document.BoardType boardType) {
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

    private void insertEvent(long documentId, String visitorKey, LocalDate viewedDate, int hour) {
        entityManager.createNativeQuery("""
                        insert into front_content_view_event
                            (document_no, visitor_key, viewed_date, viewed_dtm)
                        values (:documentNo, :visitorKey, :viewedDate, :viewedDtm)
                        """)
                .setParameter("documentNo", documentId)
                .setParameter("visitorKey", visitorKey)
                .setParameter("viewedDate", viewedDate)
                .setParameter("viewedDtm", LocalDateTime.of(viewedDate, java.time.LocalTime.of(hour, 0)))
                .executeUpdate();
    }
}
