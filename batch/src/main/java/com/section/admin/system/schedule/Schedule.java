package com.section.admin.system.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "batch.document-stats.enabled", havingValue = "true")
public class Schedule {

    @Scheduled(cron = "${batch.document-stats.cron:0 */10 * * * *}")
    public void documentStatsSchedule() {
        // 집계 구현 전까지 명시적으로 활성화한 환경에서만 스케줄 생존 여부를 기록한다.
        log.info("Document statistics scheduler heartbeat");
    }
}
