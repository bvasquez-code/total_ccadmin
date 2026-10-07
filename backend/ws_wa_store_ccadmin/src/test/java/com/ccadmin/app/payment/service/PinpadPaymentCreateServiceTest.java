package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.exception.PinpadPaymentException;
import com.ccadmin.app.payment.model.dto.PinpadPaymentStatusDto;
import com.ccadmin.app.payment.model.dto.PinpadSignedMessageDto;
import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.repository.TrxPaymentRepository;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.model.entity.PaymentMethodEntity;
import com.ccadmin.app.system.shared.CurrencyShared;
import com.ccadmin.app.system.shared.PaymentMethodShared;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PinpadPaymentCreateServiceTest {
    static final String KEY = "test-only-shared-key-at-least-32-bytes";
    final ObjectMapper mapper = new ObjectMapper();
    final PinpadConfigurationSearchService configurations = mock(PinpadConfigurationSearchService.class);
    final TrxPaymentRepository repository = mock(TrxPaymentRepository.class);
    final TrxPaymentPinpadCreateService persistence = mock(TrxPaymentPinpadCreateService.class);
    final PaymentMethodShared methods = mock(PaymentMethodShared.class);
    final CurrencyShared currencies = mock(CurrencyShared.class);
    final PinpadSignatureService signatures = new PinpadSignatureService(mapper);
    final PinpadTrustCreateService trust = mock(PinpadTrustCreateService.class);
    static final KeyPair IDENTITY = identity();
    PinpadPaymentCreateService service;

    @BeforeEach
    void setup() {
        var connection = new PinpadConfigurationSearchService.Connection("CAJA01", "http://127.0.0.1:8094/pinpad/login",
                "http://127.0.0.1:8094/browser-pinpad", "http://127.0.0.1:8094/browser-pinpad",
                "http://127.0.0.1:8094/browser-pinpad", 130, 1000);
        when(configurations.connection("USER1", 9L, "S1")).thenReturn(connection);
        when(trust.commandSigningKey()).thenReturn(KEY);
        when(trust.agentPublicKey("CAJA01")).thenReturn(Base64.getEncoder().encodeToString(IDENTITY.getPublic().getEncoded()));
        PaymentMethodEntity method = new PaymentMethodEntity();
        method.PaymentMethodType = "1002";
        when(methods.findById("TC001")).thenReturn(method);
        CurrencyEntity currency = new CurrencyEntity();
        currency.CurrencyAbbr = "PEN";
        when(currencies.findById("001")).thenReturn(currency);
        service = new PinpadPaymentCreateService(configurations, signatures, mapper, repository, persistence, methods, currencies, trust);
    }

    @Test
    void savesYapeApprovalAndAcknowledgesAsYapeWithoutChangingInternalCode() throws Exception {
        var method = new PaymentMethodEntity(); method.PaymentMethodType = "1006";
        when(methods.findById("WD001")).thenReturn(method);
        var payment = payment(); payment.PaymentMethodCod = "WD001";
        var prepared = service.prepare(payment, "USER1", 9L, "S1");
        var command = mapper.readTree(Base64.getUrlDecoder().decode(prepared.loginCommand().payload()));
        assertEquals("YAPE", command.path("request").path("paymentMethod").asText());
        assertEquals("WD001", command.path("request").path("internalPaymentCode").asText());
        var resultNode = mapper.valueToTree(result("APPROVED"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) resultNode).put("internalPaymentCode", "WD001");
        ((com.fasterxml.jackson.databind.node.ObjectNode) resultNode).putNull("lastFour");
        payment.PinpadResult = signResult(Map.of("agentId", "CAJA01", "command", command, "result", resultNode));
        when(persistence.saveApproved(eq(payment), any())).thenAnswer(invocation -> { payment.TrxPaymentId = 55L; return payment; });
        var saved = service.pay(payment, "USER1", 9L, "S1");
        var ack = mapper.readTree(Base64.getUrlDecoder().decode(saved.PinpadAckCommand.payload()));
        assertEquals("YAPE", ack.path("request").path("paymentMethod").asText());
        assertEquals("WD001", saved.PaymentMethodCod);
    }

    @ParameterizedTest
    @CsvSource({"1002,TJ001,CARD", "1002,TD001,CARD", "1003,TC001,CARD", "1006,WD001,YAPE"})
    void translatesCatalogTypeInsteadOfInternalPaymentCode(String type, String code, String expected) throws Exception {
        var method = new PaymentMethodEntity(); method.PaymentMethodType = type;
        when(methods.findById(code)).thenReturn(method);
        var payment = payment(); payment.PaymentMethodCod = code;
        var prepared = service.prepare(payment, "USER1", 9L, "S1");
        var command = mapper.readTree(Base64.getUrlDecoder().decode(prepared.loginCommand().payload()));
        assertEquals(expected, command.path("request").path("paymentMethod").asText());
        assertEquals(code, command.path("request").path("internalPaymentCode").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1001", "1005", "1015", "9999"})
    void refusesOtherTypesBeforeIssuingAnyPinpadInstructions(String type) {
        var method = new PaymentMethodEntity(); method.PaymentMethodType = type;
        when(methods.findById("MANUAL")).thenReturn(method);
        var payment = payment(); payment.PaymentMethodCod = "MANUAL";
        assertThrows(IllegalArgumentException.class, () -> service.prepare(payment, "USER1", 9L, "S1"));
        verifyNoInteractions(configurations, trust, persistence);
    }

    @Test
    void refusesCardProofWhenCatalogMethodIsYape() throws Exception {
        var payment = payment(); payment.PinpadResult = receipt(payment, "APPROVED");
        var method = new PaymentMethodEntity(); method.PaymentMethodType = "1006";
        when(methods.findById("TC001")).thenReturn(method);
        assertThrows(IllegalArgumentException.class, () -> service.pay(payment, "USER1", 9L, "S1"));
        verifyNoInteractions(persistence);
    }

    @Test
    void preparesSavesAndAcknowledgesApprovalWithoutCashSession() throws Exception {
        var connection = configurations.connection("USER1", 9L, "S1");
        when(configurations.connection("USER1", null, "S1")).thenReturn(connection);
        var payment = payment();
        payment.AmountPaid = new BigDecimal("4252.5");
        var instructions = service.prepare(payment, "USER1", null, "S1");
        var command = mapper.readTree(Base64.getUrlDecoder().decode(instructions.loginCommand().payload()));
        assertTrue(command.path("cashSessionId").isNull());
        assertEquals("S1", command.path("storeCod").asText());
        assertEquals(425250L, command.path("request").path("amountCents").asLong());
        assertEquals("agent-CAJA01-reference-123", command.path("request").path("externalReference").asText());
        var resultNode = mapper.valueToTree(result("APPROVED"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) resultNode).put("amountCents", 425250L);
        payment.PinpadResult = signResult(Map.of("agentId", "CAJA01", "command", command, "result", resultNode));
        when(persistence.saveApproved(eq(payment), any())).thenAnswer(invocation -> { payment.TrxPaymentId = 55L; return payment; });
        var saved = service.pay(payment, "USER1", null, "S1");
        assertNull(saved.CashSessionID);
        assertNotNull(saved.PinpadAckCommand);
        var ack = mapper.readTree(Base64.getUrlDecoder().decode(saved.PinpadAckCommand.payload()));
        assertTrue(ack.path("cashSessionId").isNull());
        assertEquals("ACK", ack.path("operation").asText());
    }

    @Test
    void authorizesSpecificPcAndSessionWithoutExposingSecretOrAckGrant() throws Exception {
        var instructions = service.prepare(payment(), "USER1", 9L, "S1");
        var register = mapper.readTree(Base64.getUrlDecoder().decode(instructions.loginCommand().payload()));
        assertEquals("CAJA01", register.path("agentId").asText());
        assertEquals(9, register.path("cashSessionId").asLong());
        assertEquals(5525, register.path("request").path("amountCents").asLong());
        assertEquals("REGISTER", register.path("operation").asText());
        assertEquals("http://127.0.0.1:8094/pinpad/login", instructions.loginUrl());
        assertFalse(register.has("centralPaymentCod"));
        assertFalse(mapper.writeValueAsString(instructions).contains(KEY));
        verifyNoInteractions(persistence);
    }

    @Test
    void savesSignedApprovalBeforeIssuingAck() throws Exception {
        var payment = payment();
        payment.PinpadResult = receipt(payment, "APPROVED");
        when(persistence.saveApproved(eq(payment), any())).thenAnswer(invocation -> { payment.TrxPaymentId = 55L; return payment; });
        var result = service.pay(payment, "USER1", 9L, "S1");
        var ack = mapper.readTree(Base64.getUrlDecoder().decode(result.PinpadAckCommand.payload()));
        assertEquals("ACK", ack.path("operation").asText());
        assertEquals("55", ack.path("centralPaymentCod").asText());
        assertNull(result.PinpadResult);
        verify(persistence).saveApproved(eq(payment), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROCESSING", "UNKNOWN", "TIMEOUT", "ERROR", "READ", "REJECTED", "CANCELLED"})
    void refusesSignedUnapprovedResults(String status) throws Exception {
        var payment = payment();
        payment.PinpadResult = receipt(payment, status);
        var error = assertThrows(PinpadPaymentException.class, () -> service.pay(payment, "USER1", 9L, "S1"));
        assertEquals(status, error.PaymentStatus);
        assertEquals("REJECTED".equals(status) || "CANCELLED".equals(status), error.CanStartNewPayment);
        assertNull(payment.PinpadAckCommand);
        verifyNoInteractions(persistence);
    }

    @Test
    void refusesBrowserApprovalWithoutProof() {
        assertThrows(PinpadPaymentException.class, () -> service.pay(payment(), "USER1", 9L, "S1"));
        verifyNoInteractions(persistence);
    }

    @Test
    void refusesForgedProofAndModifiedAmount() throws Exception {
        var payment = payment();
        var proof = receipt(payment, "APPROVED");
        payment.PinpadResult = new PinpadSignedMessageDto(proof.payload(), "forged");
        assertThrows(IllegalArgumentException.class, () -> service.pay(payment, "USER1", 9L, "S1"));
        payment.PinpadResult = proof;
        payment.AmountPaid = BigDecimal.ONE;
        assertThrows(IllegalArgumentException.class, () -> service.pay(payment, "USER1", 9L, "S1"));
        verifyNoInteractions(persistence);
    }

    @Test
    void refusesAnotherPcAndCashSession() throws Exception {
        var payment = payment();
        var valid = receipt(payment, "APPROVED");
        var node = mapper.readTree(Base64.getUrlDecoder().decode(valid.payload()));
        ((com.fasterxml.jackson.databind.node.ObjectNode) node.path("command")).put("cashSessionId", 10L);
        payment.PinpadResult = signResult(node);
        assertThrows(IllegalArgumentException.class, () -> service.pay(payment, "USER1", 9L, "S1"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("agentId", "CAJA02");
        payment.PinpadResult = signResult(node);
        assertThrows(IllegalArgumentException.class, () -> service.pay(payment, "USER1", 9L, "S1"));
        verifyNoInteractions(persistence);
    }

    @Test
    void refusesProofFromAnotherStore() throws Exception {
        var payment = payment();
        var node = mapper.readTree(Base64.getUrlDecoder().decode(receipt(payment, "APPROVED").payload()));
        ((com.fasterxml.jackson.databind.node.ObjectNode) node.path("command")).put("storeCod", "OTHER");
        payment.PinpadResult = signResult(node);
        assertThrows(IllegalArgumentException.class, () -> service.pay(payment, "USER1", 9L, "S1"));
        verifyNoInteractions(persistence);
    }

    @Test
    void recoversCommittedPaymentAndIssuesFreshAckWithoutResaving() {
        when(repository.findByPinpadPaymentId("reference-123")).thenReturn(saved());
        var result = service.pay(payment(), "USER1", 9L, "S1");
        assertEquals(55L, result.TrxPaymentId);
        assertNotNull(result.PinpadAckCommand);
        verifyNoInteractions(persistence);
    }

    @Test
    void lostDatabaseWriteKeepsReferenceAndProofWithoutAck() throws Exception {
        var payment = payment();
        payment.PinpadResult = receipt(payment, "APPROVED");
        when(persistence.saveApproved(any(), any())).thenThrow(new IllegalStateException("database unavailable"));
        var error = assertThrows(PinpadPaymentException.class, () -> service.pay(payment, "USER1", 9L, "S1"));
        assertEquals("APPROVED", error.PaymentStatus);
        assertFalse(error.CanStartNewPayment);
        assertNotNull(payment.PinpadResult);
        assertNull(payment.PinpadAckCommand);
    }

    @Test
    void returnsConcurrentUniqueReferenceWinner() throws Exception {
        var payment = payment();
        payment.PinpadResult = receipt(payment, "APPROVED");
        when(repository.findByPinpadPaymentId("reference-123")).thenReturn(null, saved());
        when(persistence.saveApproved(any(), any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertEquals(55L, service.pay(payment, "USER1", 9L, "S1").TrxPaymentId);
    }

    @Test
    void refusesReferenceWithAnotherAmount() {
        when(repository.findByPinpadPaymentId("reference-123")).thenReturn(saved());
        var payment = payment();
        payment.AmountPaid = BigDecimal.ONE;
        assertThrows(IllegalArgumentException.class, () -> service.pay(payment, "USER1", 9L, "S1"));
    }

    @Test
    void refusesExcessDecimalsAndChangeBeforeAuthorization() {
        var imprecise = payment();
        imprecise.AmountPaid = new BigDecimal("55.251");
        assertThrows(IllegalArgumentException.class, () -> service.prepare(imprecise, "USER1", 9L, "S1"));
        var change = payment();
        change.AmountReturned = BigDecimal.ONE;
        assertThrows(IllegalArgumentException.class, () -> service.prepare(change, "USER1", 9L, "S1"));
    }

    PinpadSignedMessageDto receipt(TrxPaymentEntity payment, String status) throws Exception {
        var prepared = service.prepare(payment, "USER1", 9L, "S1");
        var command = mapper.readTree(Base64.getUrlDecoder().decode(prepared.loginCommand().payload()));
        return signResult(Map.of("agentId", "CAJA01", "command", command, "result", result(status)));
    }
    PinpadSignedMessageDto signResult(Object data) throws Exception {
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(data));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(IDENTITY.getPrivate());
        signature.update(("CCADMIN-PINPAD-V2:RESULT:" + payload).getBytes(StandardCharsets.UTF_8));
        return new PinpadSignedMessageDto(payload, Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign()));
    }
    static KeyPair identity() {
        try { var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); return generator.generateKeyPair(); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    static TrxPaymentEntity payment() {
        var payment = new TrxPaymentEntity();
        payment.PinpadPaymentId = "reference-123";
        payment.PaymentPlatform = "POS";
        payment.PaymentMethodCod = "TC001";
        payment.TypeMovement = "I";
        payment.CurrencyCod = "001";
        payment.AmountPaid = new BigDecimal("55.25");
        payment.AmountReturned = BigDecimal.ZERO;
        return payment;
    }
    static TrxPaymentEntity saved() {
        var payment = payment();
        payment.TrxPaymentId = 55L;
        payment.CreationUser = "USER1";
        payment.CashSessionID = 9L;
        payment.PaymentStatus = "OK";
        return payment;
    }
    static PinpadPaymentStatusDto result(String status) {
        return new PinpadPaymentStatusDto("reference-123", status, 5525L, "PEN", "txn-55", "AUTH", "REF",
                "TERM1", "MERCH1", "VISA", "CREDIT", "1234", "USER1", "TC001");
    }
}
