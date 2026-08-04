package com.ccadmin.app.sunat.identity.provider.dni.eldni;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "eldni.identity")
public class ElDniIdentityProperties {

    private URI queryUrl;
    private Duration connectTimeout = Duration.ofSeconds(20);
    private Duration requestTimeout = Duration.ofSeconds(30);
    private String userAgent;

    public URI getQueryUrl() {
        if (this.queryUrl == null) {
            throw new IllegalStateException("La URL de consulta alternativa de DNI es obligatoria.");
        }
        return this.queryUrl;
    }

    public void setQueryUrl(URI queryUrl) {
        this.queryUrl = queryUrl;
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
        if (this.userAgent == null || this.userAgent.isBlank()) {
            throw new IllegalStateException(
                    "El user-agent de la consulta alternativa de DNI es obligatorio."
            );
        }
        return this.userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String origin() {
        URI url = getQueryUrl();
        int port = url.getPort();
        return url.getScheme() + "://" + url.getHost() + (port < 0 ? "" : ":" + port);
    }
}
