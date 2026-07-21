package com.section.admin.content.service;

import com.section.admin.content.res.ContentDailyStatsResponse;
import com.section.common.content.dto.DocumentDailyStatsRow;
import com.section.common.content.entity.DocumentDailyStatsSnapshot;
import com.section.common.content.repository.DocumentDailyStatsSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminContentStatsServiceTest {

    private final DocumentDailyStatsSnapshotRepository repository = mock(DocumentDailyStatsSnapshotRepository.class);
    private final AdminContentStatsService service = new AdminContentStatsService(repository);

    @Test
    @DisplayName("가장 최근 날짜의 문서 통계 스냅샷을 반환한다")
    void returnsLatestSnapshotDateItems() {
        LocalDate snapshotDate = LocalDate.of(2026, 7, 21);
        DocumentDailyStatsSnapshot total = snapshot(
                snapshotDate,
                DocumentDailyStatsSnapshot.Scope.TOTAL,
                new DocumentDailyStatsRow(null, 8L, 7L, 1L, 7L, 1L, 2L, 4L, 120L)
        );
        when(repository.findTopByOrderBySnapshotDateDesc()).thenReturn(Optional.of(total));
        when(repository.findAllBySnapshotDateOrderByScopeAsc(snapshotDate)).thenReturn(List.of(total));

        ContentDailyStatsResponse response = service.getLatestStats();

        assertThat(response.snapshotDate()).isEqualTo("2026-07-21");
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.scope()).isEqualTo("TOTAL");
            assertThat(item.totalCount()).isEqualTo(8);
            assertThat(item.totalViewCount()).isEqualTo(120);
        });
    }

    @Test
    @DisplayName("통계 스냅샷이 없으면 빈 응답을 반환한다")
    void returnsEmptyResponseWhenSnapshotMissing() {
        when(repository.findTopByOrderBySnapshotDateDesc()).thenReturn(Optional.empty());

        ContentDailyStatsResponse response = service.getLatestStats();

        assertThat(response.snapshotDate()).isNull();
        assertThat(response.items()).isEmpty();
    }

    private DocumentDailyStatsSnapshot snapshot(
            LocalDate date,
            DocumentDailyStatsSnapshot.Scope scope,
            DocumentDailyStatsRow row
    ) {
        return DocumentDailyStatsSnapshot.create(date, scope, row, LocalDateTime.of(2026, 7, 21, 14, 30));
    }
}
