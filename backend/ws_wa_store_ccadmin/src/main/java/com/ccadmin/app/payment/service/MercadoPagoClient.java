package com.ccadmin.app.payment.service;

import com.ccadmin.app.delivery.model.dto.MercadoPagoRequestDto;
import com.ccadmin.app.payment.model.entity.MercadoPagoAttemptEntity;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MercadoPagoClient {
    private static final String API = "https://api.mercadopago.com";
    private final RestTemplate restTemplate;

    public MercadoPagoClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(20000);
        restTemplate = new RestTemplate(factory);
    }

    MercadoPagoClient(RestTemplate restTemplate) { this.restTemplate = restTemplate; }

    public JsonNode create(MercadoPagoAttemptEntity attempt, MercadoPagoRequestDto.CardData form,
                           BusinessConfigEntity config) {
        validateCredentials(attempt, config);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", form.token);
        body.put("transaction_amount", attempt.Amount);
        body.put("payment_method_id", form.payment_method_id);
        body.put("installments", form.installments);
        if (form.issuer_id != null && !form.issuer_id.isBlank()) body.put("issuer_id", form.issuer_id);
        body.put("payer", form.payer);
        body.put("description", "Pedido " + attempt.SaleCod);
        body.put("external_reference", attempt.AttemptId);
        var headers = headers(config);
        headers.set("X-Idempotency-Key", attempt.AttemptId);
        try {
            return restTemplate.exchange(API + "/v1/payments", HttpMethod.POST,
                    new HttpEntity<>(body, headers), JsonNode.class).getBody();
        } catch (HttpClientErrorException exception) {
            // These responses definitively reject creation. 408/409/429 remain unresolved.
            if (java.util.Set.of(400, 401, 403, 404, 422).contains(exception.getStatusCode().value())) {
                throw new PaymentRejectedException();
            }
            throw unavailable();
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    public JsonNode find(MercadoPagoAttemptEntity attempt, BusinessConfigEntity config) {
        validateCredentials(attempt, config);
        String path = attempt.PaymentId == null
                ? "/v1/payments/search?external_reference=" + attempt.AttemptId
                : "/v1/payments/" + attempt.PaymentId;
        try {
            JsonNode result = restTemplate.exchange(API + path, HttpMethod.GET,
                    new HttpEntity<>(headers(config)), JsonNode.class).getBody();
            if (attempt.PaymentId != null) return result;
            JsonNode payments = result == null ? null : result.path("results");
            if (payments == null || !payments.isArray() || payments.isEmpty()) return null;
            if (payments.size() != 1) throw unavailable();
            return payments.get(0);
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private HttpHeaders headers(BusinessConfigEntity config) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(config.Str3Config.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public static String credentialHash(BusinessConfigEntity config) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(config.Str3Config.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    private void validateCredentials(MercadoPagoAttemptEntity attempt, BusinessConfigEntity config) {
        if (!credentialHash(config).equals(attempt.CredentialHash)) {
            throw new IllegalArgumentException("Restaura las credenciales originales para conciliar el pago pendiente");
        }
    }

    private IllegalStateException unavailable() {
        return new IllegalStateException("No se pudo confirmar la respuesta de Mercado Pago. Consulta el estado del pago.");
    }

    public static class PaymentRejectedException extends RuntimeException {
        public PaymentRejectedException() { super("Mercado Pago no acepto la solicitud de pago. Revisa los datos y las credenciales."); }
    }
}
