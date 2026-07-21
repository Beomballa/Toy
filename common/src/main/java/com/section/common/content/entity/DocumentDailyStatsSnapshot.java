package com.section.common.content.entity;

import com.section.common.content.dto.DocumentDailyStatsRow;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "document_daily_stats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_daily_stats_date_scope",
                columnNames = {"snapshot_date", "scope"}
        ),
        indexes = @Index(
                name = "idx_document_daily_stats_date",
                columnList = "snapshot_date"
        )
)
public class DocumentDailyStatsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_no")
    private Long statsNo;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private Scope scope;

    @Column(name = "total_count", nullable = false)
    private long totalCount;

    @Column(name = "published_count", nullable = false)
    private long publishedCount;

    @Column(name = "draft_count", nullable = false)
    private long draftCount;

    @Column(name = "public_count", nullable = false)
    private long publicCount;

    @Column(name = "private_count", nullable = false)
    private long privateCount;

    @Column(name = "pinned_count", nullable = false)
    private long pinnedCount;

    @Column(name = "linked_count", nullable = false)
    private long linkedCount;

    @Column(name = "total_view_count", nullable = false)
    private long totalViewCount;

    @Column(name = "aggregated_at", nullable = false)
    private LocalDateTime aggregatedAt;

    public static DocumentDailyStatsSnapshot create(
            LocalDate snapshotDate,
            Scope scope,
            DocumentDailyStatsRow row,
            LocalDateTime aggregatedAt
    ) {
        DocumentDailyStatsSnapshot snapshot = new DocumentDailyStatsSnapshot();
        snapshot.snapshotDate = snapshotDate;
        snapshot.scope = scope;
        snapshot.update(row, aggregatedAt);
        return snapshot;
    }

    public void update(DocumentDailyStatsRow row, LocalDateTime aggregatedAt) {
        this.totalCount = row.totalCount();
        this.publishedCount = row.publishedCount();
        this.draftCount = row.draftCount();
        this.publicCount = row.publicCount();
        this.privateCount = row.privateCount();
        this.pinnedCount = row.pinnedCount();
        this.linkedCount = row.linkedCount();
        this.totalViewCount = row.totalViewCount();
        this.aggregatedAt = aggregatedAt;
    }

    public enum Scope {
        TOTAL,
        NOTICE,
        STYLE,
        DISCUSS,
        QNA;

        public static Scope from(Document.BoardType boardType) {
            return Scope.valueOf(boardType.name());
        }
    }
}
