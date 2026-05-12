package com.section.common.base.entity.type.config;

import com.section.common.system.support.AdminRequestContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        // 인증 체계가 아직 완전히 붙지 않은 동안에도 요청 컨텍스트에 관리자 번호가 있으면 그 값을 우선 사용한다.
        return () -> AdminRequestContext.getCurrentAdminNo().or(() -> Optional.of(1L));
    }
}
