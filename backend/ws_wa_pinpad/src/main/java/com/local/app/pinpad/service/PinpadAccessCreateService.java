package com.local.app.pinpad.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.enums.PinpadErrorCode;
import com.local.app.pinpad.exception.PinpadPaymentException;
import com.local.app.pinpad.model.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PinpadAccessCreateService {
    private record AccessSession(JsonNode command, long expiresAt) {}
    private final Map<String, AccessSession> sessions = new ConcurrentHashMap<>();
    private final PinpadAgentProperties pinpadAgentProperties;
    private final PinpadIdentityCreateService pinpadIdentityCreateService;
    private final RestClient restClient;
    public PinpadAccessCreateService(PinpadAgentProperties pinpadAgentProperties,
            PinpadIdentityCreateService pinpadIdentityCreateService) {
        this.pinpadAgentProperties = pinpadAgentProperties; this.pinpadIdentityCreateService = pinpadIdentityCreateService;
        var factory = new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(10000); factory.setReadTimeout(10000);
        restClient = RestClient.builder().requestFactory(factory).build();
    }
    public synchronized PinpadLoginResponseDto login(PinpadLoginRequestDto request) {
        if (request == null || request.authorization() == null || request.applicationToken() == null
                || request.applicationToken().isBlank() || request.applicationToken().length() > 16384) throw unauthorized();
        URI endpoint = authorizationEndpoint();
        JsonNode response;
        try {
            String bearer = request.applicationToken().startsWith("Bearer ") ? request.applicationToken() : "Bearer " + request.applicationToken();
            response = restClient.post().uri(endpoint).header("Authorization", bearer)
                    .body(Map.of("authorization", request.authorization(), "agentId", pinpadAgentProperties.getAgentId(),
                            "publicKey", pinpadIdentityCreateService.publicKey()))
                    .retrieve().body(JsonNode.class);
        } catch (Exception exception) { throw unauthorized(); }
        if (response == null || response.path("ErrorStatus").asBoolean(true)) throw unauthorized();
        JsonNode command = response.path("Data");
        long now = Instant.now().getEpochSecond();
        long expiresAt = command.path("expiresAt").asLong(0);
        if (!pinpadAgentProperties.getAgentId().equals(command.path("agentId").asText())
                || expiresAt <= now || expiresAt > now + 900
                || !Set.of("REGISTER", "ACK").contains(command.path("operation").asText())) throw unauthorized();
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        if (sessions.size() >= 1000) throw unauthorized();
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new AccessSession(command.deepCopy(), expiresAt));
        return new PinpadLoginResponseDto(token, expiresAt);
    }
    public JsonNode command(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() > 128) throw unauthorized();
        String token = authorization.substring(7);
        var session = sessions.get(token);
        if (session == null || session.expiresAt() <= Instant.now().getEpochSecond()) {
            sessions.remove(token); throw unauthorized();
        }
        return session.command().deepCopy();
    }
    private URI authorizationEndpoint() {
        try {
            URI uri = URI.create(pinpadAgentProperties.getBackendAuthorizationUrl());
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null || uri.getQuery() != null
                    || !("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme())
                    && Set.of("localhost", "127.0.0.1").contains(uri.getHost())))) throw new IllegalArgumentException();
            return uri;
        } catch (Exception exception) { throw new PinpadPaymentException(PinpadErrorCode.INVALID_REQUEST,
                "Configurar backend-authorization-url con la URL completa HTTPS del backend o HTTP local"); }
    }
    private PinpadPaymentException unauthorized() {
        return new PinpadPaymentException(PinpadErrorCode.UNAUTHORIZED_AGENT,
                "No se pudo autorizar el acceso al pinpad. Compruebe la sesion web, la caja y la conexion con el backend", HttpStatus.UNAUTHORIZED);
    }
}
