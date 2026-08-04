package com.ccadmin.app.sunat.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "sunat.identity")
public class SunatIdentityProperties {

    private URI baseUrl;
    private Duration connectTimeout = Duration.ofSeconds(20);
    private Duration requestTimeout = Duration.ofSeconds(40);
    private String userAgent;

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public URI resolve(String path) {
        if (this.baseUrl == null) {
            throw new IllegalStateException("La URL base de SUNAT es obligatoria.");
        }

        String base = this.baseUrl.toString().replaceFirst("/+$", "");
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(base + normalizedPath);
    }
}
