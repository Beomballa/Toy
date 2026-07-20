package com.section.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BatchApplicationContextTest {

    @Test
    @DisplayName("배치 애플리케이션 컨텍스트가 감사 설정 중복 없이 기동된다")
    void contextLoads() {
    }
}
