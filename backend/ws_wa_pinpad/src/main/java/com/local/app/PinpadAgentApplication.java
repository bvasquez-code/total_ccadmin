package com.local.app;

import com.local.app.pinpad.service.PinpadInstallationCreateService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import java.nio.file.Path;

@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan
public class PinpadAgentApplication {

    public static void main(String[] args) {
        new PinpadInstallationCreateService().prepare(Path.of("."));
        SpringApplication.run(PinpadAgentApplication.class, args);
    }
}
