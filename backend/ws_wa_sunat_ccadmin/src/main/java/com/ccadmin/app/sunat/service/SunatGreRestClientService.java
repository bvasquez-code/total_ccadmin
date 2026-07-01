package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.dto.SunatSoapResponseDto;
import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import com.ccadmin.app.sunat.model.entity.SunatDocumentEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class SunatGreRestClientService {

    private static final String GUIDE_SCOPE = "https://api-cpe.sunat.gob.pe";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SunatSoapResponseDto sendGuide(SunatConfigEntity config, SunatDocumentEntity document,
                                          String fileName, byte[] zipContent) {
        try {
            validateGuideConfig(config);
            String token = token(config);
            String endpoint = buildDocumentEndpoint(config, document);
            Map<String, Object> payload = new LinkedHashMap<>();
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("nomArchivo", fileName);
            file.put("arcGreZip", Base64.getEncoder().encodeToString(zipContent));
            file.put("hashZip", sha256(zipContent));
            payload.put("archivo", file);
            return postJson(endpoint, token, this.objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            return failure(config == null ? null : config.GuideEndpoint, "Error enviando guia GRE REST", ex);
        }
    }

    public SunatSoapResponseDto getStatus(SunatConfigEntity config, String ticket) {
        try {
            validateGuideConfig(config);
            if (ticket == null || ticket.isBlank()) {
                throw new IllegalArgumentException("Ticket GRE requerido");
            }
            String token = token(config);
            String endpoint = normalizeBase(config.GuideEndpoint) + "/comprobantes/envios/" + url(ticket);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return parse(endpoint, httpResponse.statusCode(), httpResponse.body());
        } catch (Exception ex) {
            return failure(config == null ? null : config.GuideEndpoint, "Error consultando ticket GRE REST", ex);
        }
    }

    private String token(SunatConfigEntity config) throws Exception {
        String endpoint = config.GuideTokenEndpoint
                .replace("{client_id}", config.GuideClientId)
                .replace("<client_id>", config.GuideClientId);
        String form = "grant_type=password"
                + "&scope=" + url(GUIDE_SCOPE)
                + "&client_id=" + url(config.GuideClientId)
                + "&client_secret=" + url(config.GuideClientSecret)
                + "&username=" + url(config.IssuerRuc + config.SolUser)
                + "&password=" + url(config.SolPassword);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalArgumentException("No se pudo obtener token GRE SUNAT: " + response.body());
        }
        JsonNode root = this.objectMapper.readTree(response.body());
        JsonNode accessToken = root.get("access_token");
        if (accessToken == null || accessToken.asText().isBlank()) {
            throw new IllegalArgumentException("SUNAT no devolvio access_token GRE: " + response.body());
        }
        return accessToken.asText();
    }

    private SunatSoapResponseDto postJson(String endpoint, String token, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return parse(endpoint, response.statusCode(), response.body());
        } catch (Exception ex) {
            return failure(endpoint, "Error invocando endpoint GRE REST", ex);
        }
    }

    private SunatSoapResponseDto parse(String endpoint, int statusCode, String rawResponse) {
        SunatSoapResponseDto response = new SunatSoapResponseDto();
        response.Endpoint = endpoint;
        response.HttpStatusCode = statusCode;
        response.RawResponse = rawResponse;
        if (rawResponse == null || rawResponse.isBlank()) {
            return response;
        }
        try {
            JsonNode root = this.objectMapper.readTree(rawResponse);
            response.Ticket = text(root, "numTicket");
            response.ContentBase64 = text(root, "arcCdr");

            String codRespuesta = text(root, "codRespuesta");
            if ("99".equals(codRespuesta)) {
                JsonNode error = root.get("error");
                response.FaultCode = text(error, "numError");
                response.FaultString = text(error, "desError");
            }

            if (statusCode < 200 || statusCode >= 300) {
                response.FaultCode = firstNotBlank(text(root, "cod"), String.valueOf(statusCode));
                response.FaultString = firstNotBlank(text(root, "msg"), text(root, "message"), rawResponse);
                JsonNode errors = root.get("errors");
                if (errors != null && !errors.isEmpty()) {
                    response.FaultString = errors.toString();
                }
            }
            return response;
        } catch (Exception ex) {
            response.FaultCode = String.valueOf(statusCode);
            response.FaultString = "Respuesta REST GRE no pudo ser interpretada: " + ex.getMessage();
            return response;
        }
    }

    private String buildDocumentEndpoint(SunatConfigEntity config, SunatDocumentEntity document) {
        String correlative = String.valueOf(document.Correlative);
        return normalizeBase(config.GuideEndpoint)
                + "/comprobantes/"
                + url(document.IssuerRuc)
                + "-"
                + url(document.SunatDocumentType)
                + "-"
                + url(document.Series)
                + "-"
                + url(correlative);
    }

    private void validateGuideConfig(SunatConfigEntity config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuracion SUNAT requerida para GRE");
        }
        if (config.GuideEndpoint == null || config.GuideEndpoint.isBlank()) {
            throw new IllegalArgumentException("GuideEndpoint REST requerido para GRE");
        }
        if (config.GuideTokenEndpoint == null || config.GuideTokenEndpoint.isBlank()) {
            throw new IllegalArgumentException("GuideTokenEndpoint requerido para GRE");
        }
        if (isPlaceholder(config.GuideClientId)) {
            throw new IllegalArgumentException("GuideClientId requerido para GRE REST");
        }
        if (isPlaceholder(config.GuideClientSecret)) {
            throw new IllegalArgumentException("GuideClientSecret requerido para GRE REST");
        }
        if (config.IssuerRuc == null || config.IssuerRuc.isBlank()
                || config.SolUser == null || config.SolUser.isBlank()
                || config.SolPassword == null || config.SolPassword.isBlank()) {
            throw new IllegalArgumentException("Credenciales SOL requeridas para GRE REST");
        }
    }

    private boolean isPlaceholder(String value) {
        return value == null || value.isBlank() || value.trim().startsWith("REEMPLAZAR_");
    }

    private String normalizeBase(String endpoint) {
        String value = endpoint.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String sha256(byte[] content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content);
        return String.format("%064x", new BigInteger(1, hash));
    }

    private String url(String value) {
        return UriUtils.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String text(JsonNode node, String field) {
        if (node == null || field == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private SunatSoapResponseDto failure(String endpoint, String message, Exception ex) {
        log.error("{}: endpoint={}", message, endpoint, ex);
        SunatSoapResponseDto response = new SunatSoapResponseDto();
        response.Endpoint = endpoint;
        response.HttpStatusCode = 0;
        response.FaultCode = "CONNECTION_ERROR";
        response.FaultString = message + ": " + exceptionMessage(ex);
        response.RawResponse = stackTrace(ex);
        return response;
    }

    private String exceptionMessage(Exception ex) {
        if (ex == null) {
            return "Error desconocido";
        }
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            return ex.getMessage();
        }
        return ex.getClass().getName();
    }

    private String stackTrace(Exception ex) {
        if (ex == null) {
            return "";
        }
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
