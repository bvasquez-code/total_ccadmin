package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.model.entity.TrxPaymentDocumentEntity;
import com.ccadmin.app.payment.repository.TrxPaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TrxPaymentPinpadCreateServiceTest {
    @Test
    void persistsApprovedTransactionAndSafeEvidenceInTheSameCommand() throws Exception {
        var repository = mock(TrxPaymentRepository.class);
        var documents = mock(TrxPaymentDocumentCreateService.class);
        var mapper = new ObjectMapper();
        var service = new TrxPaymentPinpadCreateService(repository, documents, mapper);
        var payment = PinpadPaymentCreateServiceTest.payment();
        payment.CardNumber = "4111111111111111";
        payment.CardCVV = "123";
        payment.CardHolderName = "Fake client input";
        when(repository.saveAndFlush(payment)).thenAnswer(invocation -> { payment.TrxPaymentId = 55L; return payment; });
        var saved = service.saveApproved(payment, PinpadPaymentCreateServiceTest.result("APPROVED"));
        assertEquals("OK", saved.PaymentStatus);
        assertEquals("txn-55", saved.TransactionId);
        assertEquals("************1234", saved.CardNumber);
        assertNull(saved.CardCVV);
        assertNull(saved.CardHolderName);
        ArgumentCaptor<List<TrxPaymentDocumentEntity>> captured = ArgumentCaptor.forClass(List.class);
        verify(documents).save(eq(55L), captured.capture());
        var document = captured.getValue().get(0);
        assertEquals("PINPAD_RESPONSE", document.DocumentType);
        assertEquals("PINPAD", document.SourceType);
        assertEquals("APPROVED", mapper.readTree(document.Content).path("status").asText());
        assertFalse(document.Content.contains("4111111111111111"));
        assertFalse(document.Content.contains("CardCVV"));
    }

    @Test
    void cannotPersistUnapprovedResult() {
        var repository = mock(TrxPaymentRepository.class);
        var documents = mock(TrxPaymentDocumentCreateService.class);
        var service = new TrxPaymentPinpadCreateService(repository, documents, new ObjectMapper());
        assertThrows(IllegalArgumentException.class, () -> service.saveApproved(
                PinpadPaymentCreateServiceTest.payment(), PinpadPaymentCreateServiceTest.result("UNKNOWN")));
        verifyNoInteractions(repository, documents);
    }
}
