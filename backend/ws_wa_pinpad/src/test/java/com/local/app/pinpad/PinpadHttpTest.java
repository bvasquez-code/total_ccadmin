package com.local.app.pinpad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "pinpad.provider=demo",
        "pinpad.agent-token=http-test-token",
        "pinpad.agent-id=CAJA01",
        "pinpad.storage-path=target/http-test-storage",
        "pinpad.simulator-min-delay-millis=0",
        "pinpad.simulator-max-delay-millis=0"
})
@AutoConfigureMockMvc
class PinpadHttpTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.local.app.pinpad.service.PinpadAccessCreateService access;

    @org.junit.jupiter.api.BeforeEach
    void configureAccess() {
        org.mockito.Mockito.reset(access);
        org.mockito.Mockito.when(access.command(org.mockito.ArgumentMatchers.any())).thenThrow(
                new com.local.app.pinpad.exception.PinpadPaymentException(
                        com.local.app.pinpad.enums.PinpadErrorCode.UNAUTHORIZED_AGENT, "Invalid token", org.springframework.http.HttpStatus.UNAUTHORIZED));
    }

    @Test
    void loginUsesWebSessionAndDistinctRoutesRequireLocalTokenAndAllowedOrigin() throws Exception {
        mockMvc.perform(post("/pinpad/payment/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        String paymentId = "browser-" + UUID.randomUUID();
        var request = java.util.Map.of("paymentId", paymentId, "amountCents", 10000L, "currency", "PEN",
                "paymentMethod", "CARD", "cashier", "USER1", "internalPaymentCode", "TC001",
                "externalReference", "cash-session-9-" + paymentId);
        var command = objectMapper.valueToTree(java.util.Map.of("operation", "REGISTER", "agentId", "CAJA01", "cashSessionId", 9L,
                "expiresAt", java.time.Instant.now().getEpochSecond() + 900, "request", request));
        org.mockito.Mockito.doAnswer(invocation -> command.deepCopy()).when(access).command("Bearer local-token");
        org.mockito.Mockito.when(access.login(org.mockito.ArgumentMatchers.any())).thenReturn(
                new com.local.app.pinpad.model.dto.PinpadLoginResponseDto("local-token", java.time.Instant.now().getEpochSecond() + 900));
        String body = "{\"authorization\":{\"payload\":\"ticket\",\"signature\":\"signature\"},\"applicationToken\":\"web-session\"}";
        mockMvc.perform(post("/pinpad/login").header("Origin", "https://untrusted.example")
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mockMvc.perform(post("/pinpad/login").header("Origin", "http://localhost:4200")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").value("local-token"));
        mockMvc.perform(post("/pinpad/payment/register").header("Authorization", "Bearer local-token")
                        .header("Origin", "http://localhost:4200").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.signature").isString())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        mockMvc.perform(post("/pinpad/payment/ack").header("Authorization", "Bearer local-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/browser-pinpad").header("X-Agent-Token", "http-test-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void bootsWithYamlAndPreservesAuthenticatedHttpContract() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
        mockMvc.perform(get("/health").header("X-Agent-Token", "http-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("demo"))
                .andExpect(jsonPath("$.data.simulatorEnabled").value(true));
        String paymentId = "http-" + UUID.randomUUID();
        mockMvc.perform(post("/payment-pinpad").header("X-Agent-Token", "http-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"" + paymentId + "\",\"amountCents\":10001,"
                                + "\"currency\":\"PEN\",\"paymentMethod\":\"CARD\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
        mockMvc.perform(get("/payment-pinpad/{paymentId}/voucher", paymentId).header("X-Agent-Token", "http-test-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.voucher", containsString("SIMULADO")));
        mockMvc.perform(post("/payment-pinpad/{paymentId}/ack", paymentId).header("X-Agent-Token", "http-test-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("READ"));
        mockMvc.perform(get("/payment-pinpad/{paymentId}/search", paymentId).header("X-Agent-Token", "http-test-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.found").value(true))
                .andExpect(jsonPath("$.data.payment.status").value("READ"));
    }
}
