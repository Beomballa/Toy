package com.section.admin.system.service;

import com.section.common.content.dto.DocumentDailyStatsRow;
import com.section.common.content.entity.Document;
import com.section.common.content.entity.DocumentDailyStatsSnapshot;
import com.section.common.content.entity.DocumentDailyStatsSnapshot.Scope;
import com.section.common.content.repository.DocumentDailyStatsSnapshotRepository;
import com.section.common.content.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminBatchService {

    private final DocumentRepository documentRepository;
    private final DocumentDailyStatsSnapshotRepository snapshotRepository;
    private final Clock clock;

    @Autowired
    public AdminBatchService(
            DocumentRepository documentRepository,
            DocumentDailyStatsSnapshotRepository snapshotRepository
    ) {
        this(documentRepository, snapshotRepository, Clock.systemDefaultZone());
    }

    AdminBatchService(
            DocumentRepository documentRepository,
            DocumentDailyStatsSnapshotRepository snapshotRepository,
            Clock clock
    ) {
        this.documentRepository = documentRepository;
        this.snapshotRepository = snapshotRepository;
        this.clock = clock;
    }

    public DocumentStatsAggregationResult aggregateDocumentStats() {
        LocalDate snapshotDate = LocalDate.now(clock);
        LocalDateTime aggregatedAt = LocalDateTime.now(clock);
        Map<Document.BoardType, DocumentDailyStatsRow> groupedRows = documentRepository.getDocumentDailyStats()
                .stream()
                .collect(Collectors.toMap(DocumentDailyStatsRow::boardType, row -> row));

        List<DocumentDailyStatsRow> boardRows = Arrays.stream(Document.BoardType.values())
                .map(boardType -> groupedRows.getOrDefault(boardType, DocumentDailyStatsRow.empty(boardType)))
                .toList();
        DocumentDailyStatsRow totalRow = totalRow(boardRows);

        Map<Scope, DocumentDailyStatsSnapshot> existingSnapshots = snapshotRepository
                .findAllBySnapshotDateOrderByScopeAsc(snapshotDate)
                .stream()
                .collect(Collectors.toMap(DocumentDailyStatsSnapshot::getScope, snapshot -> snapshot));

        Map<Scope, DocumentDailyStatsRow> rowsByScope = new EnumMap<>(Scope.class);
        rowsByScope.put(Scope.TOTAL, totalRow);
        boardRows.forEach(row -> rowsByScope.put(Scope.from(row.boardType()), row));

        List<DocumentDailyStatsSnapshot> snapshots = rowsByScope.entrySet().stream()
                .map(entry -> updateOrCreate(
                        existingSnapshots.get(entry.getKey()),
                        snapshotDate,
                        entry.getKey(),
                        entry.getValue(),
                        aggregatedAt
                ))
                .toList();
        snapshotRepository.saveAll(snapshots);

        return new DocumentStatsAggregationResult(snapshotDate, totalRow.totalCount(), snapshots.size());
    }

    private DocumentDailyStatsSnapshot updateOrCreate(
            DocumentDailyStatsSnapshot existing,
            LocalDate snapshotDate,
            Scope scope,
            DocumentDailyStatsRow row,
            LocalDateTime aggregatedAt
    ) {
        if (existing == null) {
            return DocumentDailyStatsSnapshot.create(snapshotDate, scope, row, aggregatedAt);
        }
        existing.update(row, aggregatedAt);
        return existing;
    }

    private DocumentDailyStatsRow totalRow(List<DocumentDailyStatsRow> rows) {
        return new DocumentDailyStatsRow(
                null,
                sum(rows, DocumentDailyStatsRow::totalCount),
                sum(rows, DocumentDailyStatsRow::publishedCount),
                sum(rows, DocumentDailyStatsRow::draftCount),
                sum(rows, DocumentDailyStatsRow::publicCount),
                sum(rows, DocumentDailyStatsRow::privateCount),
                sum(rows, DocumentDailyStatsRow::pinnedCount),
                sum(rows, DocumentDailyStatsRow::linkedCount),
                sum(rows, DocumentDailyStatsRow::totalViewCount)
        );
    }

    private long sum(List<DocumentDailyStatsRow> rows, ToLongFunction<DocumentDailyStatsRow> extractor) {
        return rows.stream().mapToLong(extractor).sum();
    }

    public record DocumentStatsAggregationResult(
            LocalDate snapshotDate,
            long documentCount,
            int snapshotCount
    ) {
    }
}
