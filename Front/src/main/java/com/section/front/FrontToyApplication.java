package com.section.front;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.section.front", "com.section.common"})
@EntityScan(basePackages = {"com.section.common"})
@EnableJpaRepositories(basePackages = {"com.section.front", "com.section.common"})
public class FrontToyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontToyApplication.class, args);
    }
}
