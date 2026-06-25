package com.local.app.pinpad.config;

import com.local.app.pinpad.constants.PinpadConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PinpadCorsConfig implements WebMvcConfigurer {

    private final PinpadAgentProperties properties;

    public PinpadCorsConfig(PinpadAgentProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(properties.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", PinpadConstants.HEADER_AGENT_TOKEN)
                .maxAge(3600);
    }
}
