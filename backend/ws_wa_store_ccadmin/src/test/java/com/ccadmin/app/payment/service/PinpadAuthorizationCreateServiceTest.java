package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.model.dto.PinpadAgentAuthorizationRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PinpadAuthorizationCreateServiceTest {
    @TempDir Path directory;
    final ObjectMapper mapper = new ObjectMapper();
    final PinpadSignatureService signatures = new PinpadSignatureService(mapper);
    final PinpadConfigurationSearchService configurations = mock(PinpadConfigurationSearchService.class);
    Map<String, Object> command() {
        return new LinkedHashMap<>(Map.of("storeCod", "S1", "operation", "REGISTER", "agentId", "CAJA01", "cashSessionId", 9L,
                "expiresAt", Instant.now().getEpochSecond() + 900, "request", Map.of("cashier", "USER1")));
    }
    PinpadAuthorizationCreateService service(PinpadTrustCreateService trust) {
        when(configurations.connection("USER1", 9L, "S1")).thenReturn(new PinpadConfigurationSearchService.Connection(
                "CAJA01", "http://127.0.0.1:8094/pinpad/login", "http://127.0.0.1:8094/pinpad/payment/register",
                "http://127.0.0.1:8094/pinpad/payment/status", "http://127.0.0.1:8094/pinpad/payment/ack", 130, 1000));
        return new PinpadAuthorizationCreateService(signatures, trust, configurations) {
            @Override public String getUserCod() { return "USER1"; }
            @Override public String getStoreCod() { return "S1"; }
            @Override public Long getCashSessionID() { return 9L; }
        };
    }
    String publicKey() { return Base64.getEncoder().encodeToString(PinpadPaymentCreateServiceTest.IDENTITY.getPublic().getEncoded()); }
    @Test void automaticallyRegistersPublicIdentityAndPreservesItAcrossBackendRestarts() {
        var trust = new PinpadTrustCreateService(directory.toString());
        var grant = signatures.sign(command(), trust.commandSigningKey());
        assertEquals("CAJA01", service(trust).authorize(new PinpadAgentAuthorizationRequestDto(grant, "CAJA01", publicKey())).path("agentId").asText());
        var restarted = new PinpadTrustCreateService(directory.toString());
        assertEquals(trust.commandSigningKey(), restarted.commandSigningKey());
        assertEquals(publicKey(), restarted.agentPublicKey("CAJA01"));
        assertThrows(IllegalArgumentException.class, () -> restarted.registerAgent("CAJA01",
                Base64.getEncoder().encodeToString(PinpadPaymentCreateServiceTest.identity().getPublic().getEncoded())));
    }
    @Test void authorizesConfiguredTerminalWithoutCashSessionButKeepsStoreAndUserBinding() {
        var trust = new PinpadTrustCreateService(directory.toString());
        service(trust);
        var connection = configurations.connection("USER1", 9L, "S1");
        when(configurations.connection("USER1", null, "S1")).thenReturn(connection);
        var service = new PinpadAuthorizationCreateService(signatures, trust, configurations) {
            @Override public String getUserCod() { return "USER1"; }
            @Override public String getStoreCod() { return "S1"; }
            @Override public Long getCashSessionID() { return null; }
        };
        var command = command(); command.put("cashSessionId", null);
        assertTrue(service.authorize(new PinpadAgentAuthorizationRequestDto(
                signatures.sign(command, trust.commandSigningKey()), "CAJA01", publicKey())).path("cashSessionId").isNull());
        command.put("storeCod", "OTHER");
        assertThrows(IllegalArgumentException.class, () -> service.authorize(new PinpadAgentAuthorizationRequestDto(
                signatures.sign(command, trust.commandSigningKey()), "CAJA01", publicKey())));
        command.put("storeCod", "S1"); command.put("request", Map.of("cashier", "OTHER"));
        assertThrows(IllegalArgumentException.class, () -> service.authorize(new PinpadAgentAuthorizationRequestDto(
                signatures.sign(command, trust.commandSigningKey()), "CAJA01", publicKey())));
    }
    @Test void refusesExpiredAlteredOtherCashSessionAndOtherPcBeforeRegisteringIdentity() {
        var trust = new PinpadTrustCreateService(directory.toString());
        var service = service(trust);
        for (String field : List.of("cashSessionId", "expiresAt", "agentId")) {
            var command = command(); command.put(field, "agentId".equals(field) ? "CAJA02" : 0L);
            var request = new PinpadAgentAuthorizationRequestDto(signatures.sign(command, trust.commandSigningKey()), "CAJA01", publicKey());
            assertThrows(IllegalArgumentException.class, () -> service.authorize(request));
        }
        assertThrows(IllegalArgumentException.class, () -> trust.agentPublicKey("CAJA01"));
        assertThrows(IllegalArgumentException.class, () -> service.authorize(new PinpadAgentAuthorizationRequestDto(
                signatures.sign(command(), "a-different-server-key-long-enough-for-hmac"), "CAJA01", publicKey())));
    }
}
