package com.ccadmin.app;

import com.ccadmin.app.sunat.identity.config.SunatIdentityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SunatIdentityProperties.class)
public class ApplicationSunatIdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationSunatIdentityApplication.class, args);
    }
}
