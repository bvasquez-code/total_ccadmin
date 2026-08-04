package com.ccadmin.app;

import com.ccadmin.app.sunat.identity.config.SunatIdentityProperties;
import com.ccadmin.app.sunat.identity.provider.dni.eldni.ElDniIdentityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        SunatIdentityProperties.class,
        ElDniIdentityProperties.class
})
public class ApplicationSunatIdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationSunatIdentityApplication.class, args);
    }
}
