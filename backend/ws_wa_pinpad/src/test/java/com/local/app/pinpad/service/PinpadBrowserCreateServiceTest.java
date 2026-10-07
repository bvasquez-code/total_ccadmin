package com.local.app.pinpad.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.enums.*;
import com.local.app.pinpad.exception.PinpadPaymentException;
import com.local.app.pinpad.model.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PinpadBrowserCreateServiceTest {
    final ObjectMapper mapper = new ObjectMapper();
    final PinpadAgentProperties properties = new PinpadAgentProperties();
    final PinpadPaymentService payments = mock(PinpadPaymentService.class);
    final PinpadAccessCreateService access = mock(PinpadAccessCreateService.class);
    final PinpadIdentityCreateService identity = mock(PinpadIdentityCreateService.class);
    PinpadBrowserCreateService service;
    Map<String, Object> grant;
    @BeforeEach void setup() {
        properties.setAgentId("CAJA01");
        grant = command("REGISTER");
        when(access.command("Bearer valid")).thenAnswer(invocation -> mapper.valueToTree(grant));
        when(identity.signResult(anyString())).thenReturn("agent-proof");
        service = new PinpadBrowserCreateService(payments, properties, mapper, access, identity);
    }
    @Test void registerDelegatesToExistingCoreAndSignsReceipt() throws Exception {
        when(payments.registerPayment(any())).thenReturn(detail());
        var proof = service.execute("Bearer valid", "REGISTER");
        var decoded = mapper.readTree(Base64.getUrlDecoder().decode(proof.payload()));
        assertEquals("APPROVED", decoded.path("result").path("status").asText());
        assertEquals("agent-proof", proof.signature());
        verify(payments, times(1)).registerPayment(any());
    }
    @Test void statusDoesNotChargeAndCannotAcknowledgeBeforeCentralCommit() {
        when(payments.findPayment("reference-123")).thenReturn(detail());
        assertNotNull(service.execute("Bearer valid", "STATUS"));
        assertThrows(PinpadPaymentException.class, () -> service.execute("Bearer valid", "ACK"));
        verify(payments, never()).registerPayment(any());
        verify(payments, never()).ackPayment(anyString(), any());
    }
    @Test void yapeGrantKeepsWalletAndInternalCodeThroughRegisterStatusAndAck() throws Exception {
        var request = new LinkedHashMap<>((Map<String, Object>) grant.get("request"));
        request.put("paymentMethod", "YAPE"); request.put("internalPaymentCode", "WD001"); grant.put("request", request);
        var detail = detail(); detail.setPaymentMethod(PinpadPaymentMethod.YAPE);
        detail.setInternalPaymentCode("WD001"); detail.setWalletName("YAPE");
        when(payments.registerPayment(any())).thenReturn(detail);
        when(payments.findPayment("reference-123")).thenReturn(detail);
        when(payments.ackPayment(eq("reference-123"), any())).thenReturn(detail);
        var receipt = service.execute("Bearer valid", "REGISTER");
        var decoded = mapper.readTree(Base64.getUrlDecoder().decode(receipt.payload()));
        assertEquals("YAPE", decoded.path("result").path("walletName").asText());
        assertEquals("WD001", decoded.path("result").path("internalPaymentCode").asText());
        service.execute("Bearer valid", "STATUS");
        grant.put("operation", "ACK"); grant.put("centralPaymentCod", "55");
        service.execute("Bearer valid", "ACK");
        verify(payments).registerPayment(argThat(value -> value.getPaymentMethod() == PinpadPaymentMethod.YAPE
                && "WD001".equals(value.getInternalPaymentCode())));
        verify(payments).ackPayment(eq("reference-123"), any());
    }
    @Test void registerStatusAndAckWorkWithoutCashSession() throws Exception {
        grant.put("cashSessionId", null);
        var request = new LinkedHashMap<>((Map<String, Object>) grant.get("request"));
        request.put("externalReference", "agent-CAJA01-reference-123"); grant.put("request", request);
        var detail = detail(); detail.setExternalReference("agent-CAJA01-reference-123");
        when(payments.registerPayment(any())).thenReturn(detail);
        when(payments.findPayment("reference-123")).thenReturn(detail);
        when(payments.ackPayment(eq("reference-123"), any())).thenReturn(detail);
        var receipt = service.execute("Bearer valid", "REGISTER");
        assertTrue(mapper.readTree(Base64.getUrlDecoder().decode(receipt.payload())).path("command").path("cashSessionId").isNull());
        service.execute("Bearer valid", "STATUS");
        grant.put("operation", "ACK"); grant.put("centralPaymentCod", "55");
        service.execute("Bearer valid", "ACK");
        verify(payments, times(1)).registerPayment(any());
        verify(payments, times(1)).ackPayment(eq("reference-123"), any());
    }
    @Test void rejectsAnotherCashSessionAndDifferentStoredPayment() {
        when(payments.findPayment("reference-123")).thenReturn(detail());
        grant.put("cashSessionId", 10L);
        assertThrows(PinpadPaymentException.class, () -> service.execute("Bearer valid", "STATUS"));
        grant.put("cashSessionId", 9L);
        var different = detail(); different.setAmountCents(999L);
        when(payments.findPayment("reference-123")).thenReturn(different);
        assertThrows(PinpadPaymentException.class, () -> service.execute("Bearer valid", "STATUS"));
    }
    @Test void ackGrantRequiresCentralIdentifierAndCannotRegisterAnotherCharge() {
        grant = command("ACK");
        when(payments.findPayment("reference-123")).thenReturn(detail());
        assertThrows(PinpadPaymentException.class, () -> service.execute("Bearer valid", "ACK"));
        grant.put("centralPaymentCod", "55");
        when(payments.ackPayment(eq("reference-123"), any())).thenReturn(detail());
        service.execute("Bearer valid", "ACK");
        verify(payments).ackPayment(eq("reference-123"), argThat(value -> Boolean.TRUE.equals(value.getCentralSaved())
                && "55".equals(value.getCentralPaymentCod())));
        assertThrows(PinpadPaymentException.class, () -> service.execute("Bearer valid", "REGISTER"));
        verify(payments, never()).registerPayment(any());
    }
    static Map<String, Object> command(String operation) {
        var command = new LinkedHashMap<String, Object>();
        command.put("operation", operation); command.put("agentId", "CAJA01"); command.put("cashSessionId", 9L);
        command.put("expiresAt", Instant.now().getEpochSecond() + 900);
        command.put("request", Map.of("paymentId", "reference-123", "amountCents", 5525L, "currency", "PEN",
                "paymentMethod", "CARD", "cashier", "USER1", "internalPaymentCode", "TC001",
                "externalReference", "cash-session-9-reference-123"));
        return command;
    }
    PinpadPaymentDetailDto detail() {
        var detail = new PinpadPaymentDetailDto();
        detail.setPaymentId("reference-123"); detail.setAmountCents(5525L); detail.setCurrency("PEN");
        detail.setPaymentMethod(PinpadPaymentMethod.CARD); detail.setCashier("USER1");
        detail.setInternalPaymentCode("TC001"); detail.setExternalReference("cash-session-9-reference-123");
        detail.setStatus(PinpadPaymentStatus.APPROVED);
        return detail;
    }
}
