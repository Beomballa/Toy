package com.section.admin.content.res;

import com.section.common.content.entity.DocumentDailyStatsSnapshot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ContentDailyStatsResponse(
        String snapshotDate,
        String aggregatedAt,
        List<Item> items
) {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ContentDailyStatsResponse empty() {
        return new ContentDailyStatsResponse(null, null, List.of());
    }

    public static ContentDailyStatsResponse from(List<DocumentDailyStatsSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return empty();
        }
        DocumentDailyStatsSnapshot first = snapshots.getFirst();
        LocalDateTime latestAggregatedAt = snapshots.stream()
                .map(DocumentDailyStatsSnapshot::getAggregatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(first.getAggregatedAt());
        return new ContentDailyStatsResponse(
                first.getSnapshotDate().toString(),
                latestAggregatedAt.format(DATE_TIME_FORMATTER),
                snapshots.stream().map(Item::from).toList()
        );
    }

    public record Item(
            String scope,
            long totalCount,
            long publishedCount,
            long draftCount,
            long publicCount,
            long privateCount,
            long pinnedCount,
            long linkedCount,
            long totalViewCount
    ) {
        private static Item from(DocumentDailyStatsSnapshot snapshot) {
            return new Item(
                    snapshot.getScope().name(),
                    snapshot.getTotalCount(),
                    snapshot.getPublishedCount(),
                    snapshot.getDraftCount(),
                    snapshot.getPublicCount(),
                    snapshot.getPrivateCount(),
                    snapshot.getPinnedCount(),
                    snapshot.getLinkedCount(),
                    snapshot.getTotalViewCount()
            );
        }
    }
}
