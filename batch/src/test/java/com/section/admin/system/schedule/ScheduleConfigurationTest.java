package com.section.admin.system.schedule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleConfigurationTest {

    @Test
    @DisplayName("문서 집계 스케줄은 명시적으로 활성화한 경우에만 생성된다")
    void schedulerRequiresExplicitEnablement() {
        ConditionalOnProperty condition = Schedule.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(condition).isNotNull();
        assertThat(condition.name()).containsExactly("batch.document-stats.enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isFalse();
    }

    @Test
    @DisplayName("문서 집계 주기는 운영 환경변수로 변경할 수 있다")
    void schedulerCronIsConfigurable() throws NoSuchMethodException {
        Scheduled scheduled = Schedule.class.getMethod("documentStatsSchedule").getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("${batch.document-stats.cron:0 */10 * * * *}");
    }
}
