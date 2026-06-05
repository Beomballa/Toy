package com.section.front;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.section.front")
public class FrontToyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontToyApplication.class, args);
    }
}
