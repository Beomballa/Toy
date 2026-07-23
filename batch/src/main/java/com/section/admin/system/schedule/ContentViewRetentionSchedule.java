package com.section.admin.system.schedule;

import com.section.admin.system.service.ContentViewRetentionService;
import com.section.admin.system.service.ContentViewRetentionService.RetentionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "batch.content-view-retention.enabled", havingValue = "true")
public class ContentViewRetentionSchedule {

    private final ContentViewRetentionService retentionService;
    private final int retentionDays;

    public ContentViewRetentionSchedule(
            ContentViewRetentionService retentionService,
            @Value("${batch.content-view-retention.days:180}") int retentionDays
    ) {
        this.retentionService = retentionService;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${batch.content-view-retention.cron:0 30 3 * * *}")
    public void purgeContentViewEvents() {
        RetentionResult result = retentionService.purgeExpiredEvents(retentionDays);
        log.info(
                "Content view events purged: retentionStartDate={}, retentionDays={}, orphanDeleted={}, expiredDeleted={}, totalDeleted={}",
                result.retentionStartDate(),
                result.retentionDays(),
                result.orphanDeletedCount(),
                result.expiredDeletedCount(),
                result.totalDeletedCount()
        );
    }
}
