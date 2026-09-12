package com.openpis.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.openpis")
@EnableJpaRepositories(basePackages = "com.openpis")
@EntityScan(basePackages = "com.openpis")
public class OpenPisApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenPisApplication.class, args);
    }
}
