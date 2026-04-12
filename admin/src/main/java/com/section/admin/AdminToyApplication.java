package com.section.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.section.admin", "com.section.common"})
@EntityScan(basePackages = {"com.section.common"})
@EnableJpaRepositories(basePackages = {"com.section.admin", "com.section.common"})
public class AdminToyApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminToyApplication.class, args);
	}

}
