package com.section.admin.system.service;

import com.section.common.content.dto.DocumentDailyStatsRow;
import com.section.common.content.entity.Document;
import com.section.common.content.entity.DocumentDailyStatsSnapshot;
import com.section.common.content.repository.DocumentDailyStatsSnapshotRepository;
import com.section.common.content.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBatchServiceTest {

    private DocumentRepository documentRepository;
    private DocumentDailyStatsSnapshotRepository snapshotRepository;
    private AdminBatchService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        snapshotRepository = mock(DocumentDailyStatsSnapshotRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-07-21T05:30:00Z"), ZoneId.of("Asia/Seoul"));
        service = new AdminBatchService(documentRepository, snapshotRepository, clock);
        when(snapshotRepository.findAllBySnapshotDateOrderByScopeAsc(LocalDate.of(2026, 7, 21)))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("게시판별 집계를 TOTAL과 네 가지 범위 스냅샷으로 저장한다")
    void aggregatesBoardRowsAndTotalSnapshot() {
        when(documentRepository.getDocumentDailyStats()).thenReturn(List.of(
                row(Document.BoardType.NOTICE, 4, 3, 1, 30),
                row(Document.BoardType.STYLE, 2, 2, 0, 20)
        ));

        AdminBatchService.DocumentStatsAggregationResult result = service.aggregateDocumentStats();

        ArgumentCaptor<List<DocumentDailyStatsSnapshot>> captor = listCaptor();
        verify(snapshotRepository).saveAll(captor.capture());
        assertThat(result.snapshotDate()).isEqualTo(LocalDate.of(2026, 7, 21));
        assertThat(result.documentCount()).isEqualTo(6);
        assertThat(result.snapshotCount()).isEqualTo(5);
        assertThat(captor.getValue()).hasSize(5);
        assertThat(captor.getValue())
                .filteredOn(snapshot -> snapshot.getScope() == DocumentDailyStatsSnapshot.Scope.TOTAL)
                .singleElement()
                .satisfies(snapshot -> {
                    assertThat(snapshot.getTotalCount()).isEqualTo(6);
                    assertThat(snapshot.getPublishedCount()).isEqualTo(5);
                    assertThat(snapshot.getTotalViewCount()).isEqualTo(50);
                });
    }

    @Test
    @DisplayName("문서가 없어도 TOTAL과 모든 게시판 범위를 0으로 저장한다")
    void createsZeroSnapshotsWhenDocumentsAreEmpty() {
        when(documentRepository.getDocumentDailyStats()).thenReturn(List.of());

        AdminBatchService.DocumentStatsAggregationResult result = service.aggregateDocumentStats();

        ArgumentCaptor<List<DocumentDailyStatsSnapshot>> captor = listCaptor();
        verify(snapshotRepository).saveAll(captor.capture());
        assertThat(result.documentCount()).isZero();
        assertThat(captor.getValue()).hasSize(5).allSatisfy(snapshot -> {
            assertThat(snapshot.getTotalCount()).isZero();
            assertThat(snapshot.getTotalViewCount()).isZero();
        });
    }

    @Test
    @DisplayName("같은 날짜 재실행은 기존 스냅샷을 갱신해 중복 생성을 방지한다")
    void updatesExistingSnapshotsIdempotently() {
        DocumentDailyStatsSnapshot existingTotal = DocumentDailyStatsSnapshot.create(
                LocalDate.of(2026, 7, 21),
                DocumentDailyStatsSnapshot.Scope.TOTAL,
                DocumentDailyStatsRow.empty(null),
                java.time.LocalDateTime.of(2026, 7, 21, 1, 0)
        );
        when(snapshotRepository.findAllBySnapshotDateOrderByScopeAsc(LocalDate.of(2026, 7, 21)))
                .thenReturn(List.of(existingTotal));
        when(documentRepository.getDocumentDailyStats()).thenReturn(List.of(
                row(Document.BoardType.NOTICE, 3, 2, 1, 15)
        ));

        service.aggregateDocumentStats();

        ArgumentCaptor<List<DocumentDailyStatsSnapshot>> captor = listCaptor();
        verify(snapshotRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).contains(existingTotal);
        assertThat(existingTotal.getTotalCount()).isEqualTo(3);
        assertThat(existingTotal.getTotalViewCount()).isEqualTo(15);
    }

    private DocumentDailyStatsRow row(
            Document.BoardType boardType,
            long total,
            long published,
            long draft,
            long views
    ) {
        return new DocumentDailyStatsRow(
                boardType, total, published, draft, total, 0L, 0L, 0L, views
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentCaptor<List<DocumentDailyStatsSnapshot>> listCaptor() {
        return ArgumentCaptor.forClass((Class) List.class);
    }
}
