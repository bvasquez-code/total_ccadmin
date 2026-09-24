package com.ccadmin.app.payment.service;

import com.ccadmin.app.delivery.model.dto.MercadoPagoRequestDto;
import com.ccadmin.app.payment.model.entity.MercadoPagoAttemptEntity;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MercadoPagoClientTest {
    @Test
    void submitsServerAmountAndIdempotencyKeyWithPrivateCredentialOnlyInHeader() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var client = new MercadoPagoClient(restTemplate);
        var config = config();
        var attempt = attempt(config);
        var form = new MercadoPagoRequestDto.CardData();
        form.token = "card-token";
        form.payment_method_id = "visa";
        form.installments = 1;
        form.payer = new MercadoPagoRequestDto.Payer();
        form.payer.email = "test@example.com";
        server.expect(requestTo("https://api.mercadopago.com/v1/payments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-credential"))
                .andExpect(header("X-Idempotency-Key", attempt.AttemptId))
                .andExpect(jsonPath("$.transaction_amount").value(55.25))
                .andExpect(jsonPath("$.external_reference").value(attempt.AttemptId))
                .andExpect(jsonPath("$.token").value("card-token"))
                .andExpect(jsonPath("$.access_token").doesNotExist())
                .andRespond(withSuccess("{\"id\":123,\"status\":\"approved\"}", MediaType.APPLICATION_JSON));
        assertEquals("approved", client.create(attempt, form, config).path("status").asText());
        server.verify();
    }

    @Test
    void providerErrorsDoNotLeakCredentialsAndServerFailureRemainsUncertain() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var client = new MercadoPagoClient(restTemplate);
        var config = config();
        server.expect(requestTo("https://api.mercadopago.com/v1/payments"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("private-provider-content"));
        var error = assertThrows(IllegalStateException.class,
                () -> client.create(attempt(config), new MercadoPagoRequestDto.CardData(), config));
        assertFalse(error.getMessage().contains("private-provider-content"));
        assertNull(error.getCause());
    }

    @Test
    void searchesByAttemptReferenceAfterLostResponse() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var config = config();
        var attempt = attempt(config);
        server.expect(requestTo("https://api.mercadopago.com/v1/payments/search?external_reference=" + attempt.AttemptId))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));
        assertNull(new MercadoPagoClient(restTemplate).find(attempt, config));
        server.verify();
    }

    @Test
    void cannotReplayAgainstDifferentCredentials() {
        var config = config();
        var attempt = attempt(config);
        config.Str3Config = "another-account";
        assertThrows(IllegalArgumentException.class,
                () -> new MercadoPagoClient(new RestTemplate()).find(attempt, config));
    }

    private BusinessConfigEntity config() {
        var config = new BusinessConfigEntity();
        config.Str3Config = "test-credential";
        return config;
    }

    private MercadoPagoAttemptEntity attempt(BusinessConfigEntity config) {
        var attempt = new MercadoPagoAttemptEntity();
        attempt.AttemptId = "8199c44a-e88c-4c9d-a062-73b5cf534abc";
        attempt.SaleCod = "SALE1";
        attempt.Amount = new BigDecimal("55.25");
        attempt.CredentialHash = MercadoPagoClient.credentialHash(config);
        return attempt;
    }
}
