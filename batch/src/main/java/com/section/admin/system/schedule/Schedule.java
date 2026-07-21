package com.section.admin.system.schedule;

import com.section.admin.system.service.AdminBatchService;
import com.section.admin.system.service.AdminBatchService.DocumentStatsAggregationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.document-stats.enabled", havingValue = "true")
public class Schedule {

    private final AdminBatchService adminBatchService;

    @Scheduled(cron = "${batch.document-stats.cron:0 */10 * * * *}")
    public void documentStatsSchedule() {
        DocumentStatsAggregationResult result = adminBatchService.aggregateDocumentStats();
        log.info(
                "Document statistics aggregated: date={}, documents={}, snapshots={}",
                result.snapshotDate(),
                result.documentCount(),
                result.snapshotCount()
        );
    }
}
