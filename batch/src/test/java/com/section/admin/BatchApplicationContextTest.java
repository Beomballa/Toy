package com.section.admin;

import com.section.admin.system.schedule.Schedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BatchApplicationContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("배치 애플리케이션 컨텍스트가 감사 설정 중복 없이 기동된다")
    void contextLoads() {
    }

    @Test
    @DisplayName("문서 집계 스케줄은 기본 설정에서 실행되지 않는다")
    void documentStatsSchedulerIsDisabledByDefault() {
        assertThat(applicationContext.getBeansOfType(Schedule.class)).isEmpty();
    }
}
