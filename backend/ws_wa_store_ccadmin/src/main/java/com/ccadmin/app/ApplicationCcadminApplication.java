package com.ccadmin.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApplicationCcadminApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApplicationCcadminApplication.class, args);
	}

}
