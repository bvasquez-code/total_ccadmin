package com.ccadmin.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ApplicationSunatCcadminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationSunatCcadminApplication.class, args);
    }
}
