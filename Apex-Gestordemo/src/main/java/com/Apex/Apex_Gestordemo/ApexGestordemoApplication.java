package com.Apex.Apex_Gestordemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.Apex.Apex_Gestordemo", "Controller", "Service", "Repository", "Model", "DTO"})
@EntityScan("Model")
@EnableJpaRepositories("Repository")
@EnableAsync
public class ApexGestordemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApexGestordemoApplication.class, args);
    }
}
