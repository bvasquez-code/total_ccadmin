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
        "pinpad.storage-path=target/http-test-storage",
        "pinpad.simulator-min-delay-millis=0",
        "pinpad.simulator-max-delay-millis=0"
})
@AutoConfigureMockMvc
class PinpadHttpTest {
    @Autowired private MockMvc mockMvc;

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
