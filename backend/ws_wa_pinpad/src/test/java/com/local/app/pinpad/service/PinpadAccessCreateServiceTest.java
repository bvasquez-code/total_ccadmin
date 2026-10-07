package com.local.app.pinpad.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.exception.PinpadPaymentException;
import com.local.app.pinpad.model.dto.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PinpadAccessCreateServiceTest {
    final ObjectMapper mapper = new ObjectMapper();
    final PinpadAgentProperties properties = new PinpadAgentProperties();
    final PinpadIdentityCreateService identity = mock(PinpadIdentityCreateService.class);
    HttpServer server;
    int responseStatus = 200;
    Map<String, Object> grant;
    final AtomicReference<String> receivedAuthorization = new AtomicReference<>();
    @BeforeEach void setup() throws Exception {
        properties.setAgentId("CAJA01"); when(identity.publicKey()).thenReturn("public-only");
        grant = PinpadBrowserCreateServiceTest.command("REGISTER");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/exact/custom/authorization", exchange -> {
            receivedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            var request = mapper.readTree(exchange.getRequestBody().readAllBytes());
            byte[] body = mapper.writeValueAsBytes(Map.of("ErrorStatus", responseStatus != 200, "Data", grant));
            if (!"public-only".equals(request.path("publicKey").asText())) throw new AssertionError();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        properties.setBackendAuthorizationUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/exact/custom/authorization");
    }
    @AfterEach void stop() { server.stop(0); }
    PinpadLoginRequestDto request() { return new PinpadLoginRequestDto(new PinpadSignedMessageDto("ticket", "signature"), "Bearer existing-web-session"); }
    @Test void validatesExistingWebSessionAndIssuesScopedOpaqueLocalTokenWithoutPasswords() {
        var service = new PinpadAccessCreateService(properties, identity);
        var login = service.login(request());
        assertEquals("Bearer existing-web-session", receivedAuthorization.get());
        assertTrue(login.accessToken().length() >= 32);
        assertFalse(login.accessToken().contains("existing-web-session"));
        assertEquals("reference-123", service.command("Bearer " + login.accessToken()).path("request").path("paymentId").asText());
        assertThrows(PinpadPaymentException.class, () -> service.command("Bearer invented"));
        assertThrows(PinpadPaymentException.class, () -> service.command(null));
    }
    @Test void refusesInvalidWebSessionExpiredGrantOtherPcAndRemoteHttp() {
        var service = new PinpadAccessCreateService(properties, identity);
        responseStatus = 401;
        assertThrows(PinpadPaymentException.class, () -> service.login(request()));
        responseStatus = 200; grant.put("expiresAt", 1L);
        assertThrows(PinpadPaymentException.class, () -> service.login(request()));
        grant = PinpadBrowserCreateServiceTest.command("REGISTER"); grant.put("agentId", "CAJA02");
        assertThrows(PinpadPaymentException.class, () -> service.login(request()));
        properties.setBackendAuthorizationUrl("http://cloud.example/authorization");
        assertThrows(PinpadPaymentException.class, () -> service.login(request()));
    }
}
