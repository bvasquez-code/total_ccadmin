package com.ccadmin.app.payment.service;

import com.ccadmin.app.delivery.model.dto.MercadoPagoRequestDto;
import com.ccadmin.app.delivery.service.SalePaymentDeliveryCreateService;
import com.ccadmin.app.payment.model.entity.MercadoPagoAttemptEntity;
import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.repository.MercadoPagoAttemptRepository;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.system.shared.PaymentMethodShared;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MercadoPagoAttemptCreateServiceTest {
    @Mock MercadoPagoAttemptRepository mercadoPagoAttemptRepository;
    @Mock SaleHeadRepository saleHeadRepository;
    @Mock SalePaymentRepository salePaymentRepository;
    @Mock SalePaymentDeliveryCreateService salePaymentDeliveryCreateService;
    @Mock PaymentMethodShared paymentMethodShared;
    @Mock EntityManager entityManager;
    @Spy ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks MercadoPagoAttemptCreateService service;
    SaleHeadEntity sale;
    BusinessConfigEntity config;

    @BeforeEach
    void setup() {
        sale = new SaleHeadEntity();
        sale.SaleCod = "SALE1";
        sale.SaleStatus = "P";
        sale.NumTotalPrice = new BigDecimal("79.90");
        sale.CurrencyCod = "PEN";
        config = new BusinessConfigEntity();
        config.Str2Config = "PEN";
        config.Str3Config = "test-access-token";
        config.Sta2Config = "S";
        config.Num1Config = 3;
    }

    @Test
    void reservesServerAmountAndReusesOnlyAnIdenticalAttempt() throws Exception {
        var request = request();
        when(saleHeadRepository.findWebSaleBySaleCodForUpdate("SALE1")).thenReturn(Optional.of(sale));
        when(mercadoPagoAttemptRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var preparation = service.prepare("SALE1", request, config);
        var attempt = preparation.attempt();
        assertTrue(preparation.created());
        assertEquals(new BigDecimal("79.90"), attempt.Amount);
        assertEquals("P", attempt.PaymentState);
        assertEquals(64, attempt.RequestHash.length());
        assertNotEquals(request.FormData.token, attempt.RequestHash);
        verify(saleHeadRepository).findWebSaleBySaleCodForUpdate("SALE1");
        when(mercadoPagoAttemptRepository.findById(request.AttemptId)).thenReturn(Optional.of(attempt));
        assertFalse(service.prepare("SALE1", request, config).created());
        request.FormData.token = "different-card-token";
        assertThrows(IllegalArgumentException.class, () -> service.prepare("SALE1", request, config));
        verify(mercadoPagoAttemptRepository, times(1)).saveAndFlush(any());
    }

    @Test
    void blocksAnotherAttemptWhileAChargeIsUnresolved() {
        when(saleHeadRepository.findWebSaleBySaleCodForUpdate("SALE1")).thenReturn(Optional.of(sale));
        when(mercadoPagoAttemptRepository.hasPendingPayment("SALE1")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.prepare("SALE1", request(), config));
        verify(mercadoPagoAttemptRepository, never()).saveAndFlush(any());
    }

    @Test
    void refusesPaidOrCancelledOrders() {
        when(saleHeadRepository.findWebSaleBySaleCodForUpdate("SALE1")).thenReturn(Optional.of(sale));
        sale.IsPaid = "S";
        assertThrows(IllegalArgumentException.class, () -> service.prepare("SALE1", request(), config));
        sale.IsPaid = "N";
        sale.SaleStatus = "A";
        assertThrows(IllegalArgumentException.class, () -> service.prepare("SALE1", request(), config));
        verify(mercadoPagoAttemptRepository, never()).saveAndFlush(any());
    }

    @Test
    void approvalDelegatesOnceAndStoresOnlyMaskedCardData() throws Exception {
        var attempt = storedAttempt();
        var payment = payment(attempt, "approved");
        payment.put("payment_type_id", "debit_card");
        service.apply(attempt.AttemptId, payment);
        service.apply(attempt.AttemptId, payment);
        var capture = ArgumentCaptor.forClass(TrxPaymentEntity.class);
        verify(salePaymentDeliveryCreateService, times(1)).saveApprovedGatewayPayment(eq(sale), capture.capture());
        assertEquals("C", attempt.PaymentState);
        assertEquals("TD001", capture.getValue().PaymentMethodCod);
        assertEquals("12345", capture.getValue().TransactionId);
        assertEquals("************1234", capture.getValue().CardNumber);
        assertNull(capture.getValue().CardCVV);
        assertNull(capture.getValue().CardExpirationDate);
    }

    @Test
    void pendingDoesNotRegisterAPaymentAndCanLaterBeApproved() throws Exception {
        var attempt = storedAttempt();
        service.apply(attempt.AttemptId, payment(attempt, "in_process"));
        assertEquals("P", attempt.PaymentState);
        verifyNoInteractions(salePaymentDeliveryCreateService);
        service.apply(attempt.AttemptId, payment(attempt, "approved"));
        assertEquals("C", attempt.PaymentState);
        verify(salePaymentDeliveryCreateService).saveApprovedGatewayPayment(eq(sale), any());
    }

    @Test
    void rejectionReleasesTheOrderWithoutRegisteringMoney() throws Exception {
        var attempt = storedAttempt();
        var payment = payment(attempt, "rejected");
        payment.put("status_detail", "cc_rejected_other_reason");
        service.apply(attempt.AttemptId, payment);
        assertEquals("F", attempt.PaymentState);
        assertEquals("rejected", attempt.ProviderStatus);
        assertEquals("cc_rejected_other_reason", attempt.ProviderStatusDetail);
        verifyNoInteractions(salePaymentDeliveryCreateService);
    }

    @Test
    void missingRemoteResultKeepsAttemptPending() throws Exception {
        var attempt = storedAttempt();
        service.apply(attempt.AttemptId, null);
        assertEquals("P", attempt.PaymentState);
        verifyNoInteractions(salePaymentDeliveryCreateService);
    }

    @Test
    void rejectsMismatchedReferenceAmountCurrencyModeAndPaymentType() throws Exception {
        var attempt = storedAttempt();
        for (String field : java.util.List.of("external_reference", "transaction_amount", "currency_id", "live_mode", "payment_type_id")) {
            var payment = payment(attempt, "approved");
            switch (field) {
                case "transaction_amount" -> payment.put(field, 1);
                case "live_mode" -> payment.put(field, true);
                default -> payment.put(field, "other");
            }
            assertThrows(IllegalStateException.class, () -> service.apply(attempt.AttemptId, payment), field);
        }
        assertEquals("P", attempt.PaymentState);
        verifyNoInteractions(salePaymentDeliveryCreateService);
    }

    @Test
    void refusesAnAttemptOwnedByAnotherOrder() {
        var attempt = new MercadoPagoAttemptEntity();
        attempt.SaleCod = "OTHER";
        var request = request();
        when(saleHeadRepository.findWebSaleBySaleCodForUpdate("SALE1")).thenReturn(Optional.of(sale));
        when(mercadoPagoAttemptRepository.findById(request.AttemptId)).thenReturn(Optional.of(attempt));
        assertThrows(IllegalArgumentException.class, () -> service.prepare("SALE1", request, config));
    }

    private MercadoPagoAttemptEntity storedAttempt() {
        var attempt = new MercadoPagoAttemptEntity();
        attempt.AttemptId = UUID.randomUUID().toString();
        attempt.SaleCod = "SALE1";
        attempt.Amount = sale.NumTotalPrice;
        attempt.CurrencyCod = "PEN";
        attempt.TestMode = "S";
        attempt.PaymentState = "P";
        when(mercadoPagoAttemptRepository.findById(attempt.AttemptId)).thenReturn(Optional.of(attempt));
        when(saleHeadRepository.findWebSaleBySaleCodForUpdate("SALE1")).thenReturn(Optional.of(sale));
        return attempt;
    }

    private ObjectNode payment(MercadoPagoAttemptEntity attempt, String status) {
        var payment = objectMapper.createObjectNode();
        payment.put("id", 12345);
        payment.put("external_reference", attempt.AttemptId);
        payment.put("transaction_amount", attempt.Amount);
        payment.put("currency_id", "PEN");
        payment.put("live_mode", false);
        payment.put("payment_type_id", "credit_card");
        payment.put("status", status);
        payment.putObject("card").put("last_four_digits", "1234");
        return payment;
    }

    private MercadoPagoRequestDto request() {
        var request = new MercadoPagoRequestDto();
        request.AttemptId = UUID.randomUUID().toString();
        request.PaymentMethodCod = "TC001";
        request.FormData = new MercadoPagoRequestDto.CardData();
        request.FormData.token = "test-card-token";
        request.FormData.payment_method_id = "visa";
        request.FormData.installments = 1;
        request.FormData.payer = new MercadoPagoRequestDto.Payer();
        request.FormData.payer.email = "buyer@example.com";
        return request;
    }
}
