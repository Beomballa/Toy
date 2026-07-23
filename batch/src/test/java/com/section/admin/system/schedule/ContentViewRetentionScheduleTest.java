package com.section.admin.system.schedule;

import com.section.admin.system.service.ContentViewRetentionService;
import com.section.admin.system.service.ContentViewRetentionService.RetentionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentViewRetentionScheduleTest {

    @Test
    @DisplayName("조회 이벤트 정리 스케줄은 설정된 보존 일수로 서비스를 호출한다")
    void invokesRetentionServiceWithConfiguredDays() {
        ContentViewRetentionService service = mock(ContentViewRetentionService.class);
        when(service.purgeExpiredEvents(180))
                .thenReturn(new RetentionResult(LocalDate.of(2026, 1, 25), 180, 2, 18));
        ContentViewRetentionSchedule schedule = new ContentViewRetentionSchedule(service, 180);

        schedule.purgeContentViewEvents();

        verify(service).purgeExpiredEvents(180);
    }

    @Test
    @DisplayName("조회 이벤트 정리 스케줄은 명시적으로 활성화해야 생성된다")
    void requiresExplicitEnablement() {
        ConditionalOnProperty condition =
                ContentViewRetentionSchedule.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(condition).isNotNull();
        assertThat(condition.name()).containsExactly("batch.content-view-retention.enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isFalse();
    }

    @Test
    @DisplayName("조회 이벤트 정리 주기는 운영 환경변수로 변경할 수 있다")
    void cronIsConfigurable() throws NoSuchMethodException {
        Scheduled scheduled = ContentViewRetentionSchedule.class
                .getMethod("purgeContentViewEvents")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled.cron())
                .isEqualTo("${batch.content-view-retention.cron:0 30 3 * * *}");
    }
}
