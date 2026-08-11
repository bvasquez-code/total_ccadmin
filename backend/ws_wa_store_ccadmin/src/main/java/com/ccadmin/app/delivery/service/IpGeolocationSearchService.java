package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.IpGeolocationDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class IpGeolocationSearchService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String providerUrl;

    public IpGeolocationSearchService(
            ObjectMapper objectMapper,
            @Value("${delivery.ip-geolocation.url:https://ipwho.is/%s?fields=success,message,latitude,longitude,city,region,country}")
            String providerUrl
    ) {
        this.objectMapper = objectMapper;
        this.providerUrl = providerUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    public IpGeolocationDto findByRequest(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        validatePublicIp(clientIp);

        try {
            HttpRequest providerRequest = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(providerUrl, clientIp)))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    providerRequest,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("El proveedor de ubicación no respondió correctamente");
            }

            JsonNode data = objectMapper.readTree(response.body());
            if (!data.path("success").asBoolean(false)
                    || !data.hasNonNull("latitude")
                    || !data.hasNonNull("longitude")) {
                throw new IllegalStateException(data.path("message").asText("No se pudo ubicar la IP"));
            }

            return new IpGeolocationDto(
                    data.path("latitude").decimalValue(),
                    data.path("longitude").decimalValue(),
                    buildAddress(data)
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La ubicación automática fue interrumpida", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException(
                    "No se pudo detectar la ubicación automáticamente. Seleccione una ubicación manual",
                    ex
            );
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void validatePublicIp(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
                throw new IllegalArgumentException(
                        "No se puede detectar la ubicación desde una IP local. Seleccione una ubicación manual"
                );
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("La IP del cliente no es válida", ex);
        }
    }

    private String buildAddress(JsonNode data) {
        List<String> parts = new ArrayList<>();
        addAddressPart(parts, data.path("city").asText());
        addAddressPart(parts, data.path("region").asText());
        addAddressPart(parts, data.path("country").asText());
        return String.join(" - ", parts);
    }

    private void addAddressPart(List<String> parts, String value) {
        if (value != null && !value.isBlank() && !parts.contains(value)) {
            parts.add(value);
        }
    }
}
