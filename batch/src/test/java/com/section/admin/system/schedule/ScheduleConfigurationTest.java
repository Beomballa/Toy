package com.section.admin.system.schedule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;

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

    @Test
    @DisplayName("스케줄러 풀 크기는 운영 환경에 맞게 조정할 수 있다")
    void schedulerPoolSizeIsConfigurable() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "test",
                Map.of("spring.task.scheduling.pool.size", "4")
        ));

        Integer poolSize = Binder.get(environment)
                .bind("spring.task.scheduling.pool.size", Bindable.of(Integer.class))
                .orElseThrow(() -> new IllegalStateException("스케줄러 풀 크기 설정이 필요합니다."));

        assertThat(poolSize).isEqualTo(4);
    }
}
