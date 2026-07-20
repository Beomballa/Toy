package com.section.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.section.admin", "com.section.common"})
@EntityScan(basePackages = {"com.section.common"})
@EnableJpaRepositories(basePackages = {"com.section.admin", "com.section.common"})
@EnableScheduling
public class AdminBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminBatchApplication.class, args);
	}

}
