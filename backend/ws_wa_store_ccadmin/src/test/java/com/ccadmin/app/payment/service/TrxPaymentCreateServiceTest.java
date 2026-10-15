package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.exception.TrxPaymentBuildException;
import com.ccadmin.app.payment.repository.TrxPaymentRepository;
import com.ccadmin.app.shared.model.dto.SessionDto;
import com.ccadmin.app.user.shared.UserStoreShared;
import com.ccadmin.app.user.model.entity.UserStoreEntity;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.shared.CurrencyShared;
import com.ccadmin.app.system.shared.PaymentMethodShared;
import com.ccadmin.app.system.model.entity.PaymentMethodEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrxPaymentCreateServiceTest {
    @Mock TrxPaymentRepository trxPaymentRepository;
    @Mock CurrencyShared currencyShared;
    @Mock PaymentMethodShared paymentMethodShared;
    @Mock UserStoreShared userStoreShared;
    @Mock PinpadPaymentCreateService pinpadPaymentCreateService;
    @Mock PinpadConfigurationSearchService pinpadConfigurationSearchService;
    @InjectMocks TrxPaymentCreateService service;

    @BeforeEach
    void setup() {
        var authentication = new UsernamePasswordAuthenticationToken("USER1", null, List.of());
        var session = new SessionDto();
        session.CashSessionID = 9L;
        session.StoreCod = "S1";
        var userStore = new UserStoreEntity(); userStore.StoreCod = "S1";
        lenient().when(userStoreShared.findByUserCod("USER1")).thenReturn(List.of(userStore));
        authentication.setDetails(session);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var currency = new CurrencyEntity();
        currency.CurrencyCod = "001";
        lenient().when(currencyShared.findCurrencySystem()).thenReturn(currency);
        lenient().when(pinpadConfigurationSearchService.isIntegrationEnabled()).thenReturn(true);
        var card = new PaymentMethodEntity(); card.PaymentProcessingMode = "I";
        lenient().when(paymentMethodShared.findById("TC001")).thenReturn(card);
        lenient().when(paymentMethodShared.findById("EF001")).thenReturn(new PaymentMethodEntity());
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void singleAndBulkPosPaymentUseSameDomainCommandWithAuthenticatedSession() {
        var single = PinpadPaymentCreateServiceTest.payment();
        when(pinpadPaymentCreateService.pay(any(), eq("USER1"), eq(9L), eq("S1")))
                .thenReturn(PinpadPaymentCreateServiceTest.saved());
        assertEquals(55L, service.save(single).TrxPaymentId);
        assertEquals(55L, service.saveAll(List.of(PinpadPaymentCreateServiceTest.payment())).get(0).TrxPaymentId);
        verify(pinpadPaymentCreateService, times(2)).pay(any(), eq("USER1"), eq(9L), eq("S1"));
        verifyNoInteractions(trxPaymentRepository);
        assertEquals("001", single.CurrencyCodSys);
    }

    @Test
    void cashAndAccountingReversalsDoNotStartPhysicalCharge() {
        var cash = PinpadPaymentCreateServiceTest.payment();
        cash.PinpadPaymentId = null;
        cash.PaymentPlatform = "FISICO";
        cash.PaymentMethodCod = "EF001";
        when(trxPaymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertSame(cash, service.save(cash));
        var reversal = TrxPaymentEntity.buildReversal(cash, "USER1");
        assertSame(reversal, service.save(reversal));
        verifyNoInteractions(pinpadPaymentCreateService);
        verify(trxPaymentRepository, times(2)).save(any());
    }

    @Test
    void nonPosCannotInjectPinpadReferenceThroughBulkSave() {
        var payment = PinpadPaymentCreateServiceTest.payment();
        payment.PaymentPlatform = "FISICO";
        assertThrows(TrxPaymentBuildException.class, () -> service.saveAll(List.of(payment)));
        verifyNoInteractions(pinpadPaymentCreateService, trxPaymentRepository);
    }

    @Test
    void directYapeDoesNotInvokePinpadAndBothAllowsEitherChannel() {
        var method = new PaymentMethodEntity(); method.PaymentProcessingMode = "B";
        when(paymentMethodShared.findById("WD001")).thenReturn(method);
        var payment = PinpadPaymentCreateServiceTest.payment(); payment.PaymentMethodCod = "WD001";
        payment.PaymentPlatform = "FISICO"; payment.PinpadPaymentId = null;
        when(trxPaymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertSame(payment, service.save(payment));
        verifyNoInteractions(pinpadPaymentCreateService);
        payment.PaymentPlatform = "POS"; payment.PinpadPaymentId = "reference-123";
        when(pinpadPaymentCreateService.pay(eq(payment), eq("USER1"), eq(9L), eq("S1"))).thenReturn(payment);
        assertSame(payment, service.save(payment));
    }

    @Test
    void directOnlyRefusesPinpadAndIntermediaryOnlyRefusesDirectInSingleAndBulkSave() {
        var method = new PaymentMethodEntity(); method.PaymentProcessingMode = "D";
        when(paymentMethodShared.findById("WD001")).thenReturn(method);
        var payment = PinpadPaymentCreateServiceTest.payment(); payment.PaymentMethodCod = "WD001";
        assertThrows(TrxPaymentBuildException.class, () -> service.save(payment));
        assertThrows(TrxPaymentBuildException.class, () -> service.saveAll(List.of(payment)));
        method.PaymentProcessingMode = "I";
        payment.PaymentPlatform = "FISICO"; payment.PinpadPaymentId = null;
        assertThrows(TrxPaymentBuildException.class, () -> service.save(payment));
        assertThrows(TrxPaymentBuildException.class, () -> service.saveAll(List.of(payment)));
        verifyNoInteractions(pinpadPaymentCreateService, trxPaymentRepository);
    }

    @Test
    void unintegratedTerminalCommitsManualConfirmationThroughSingleAndBulkFlowWithoutAgent() {
        when(pinpadConfigurationSearchService.isIntegrationEnabled()).thenReturn(false);
        var payment = PinpadPaymentCreateServiceTest.payment();
        payment.PaymentPlatform = "POS_MANUAL"; payment.PinpadPaymentId = null;
        when(trxPaymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(trxPaymentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals("OK", service.save(payment).PaymentStatus);
        assertEquals("POS_MANUAL", service.saveAll(List.of(payment)).get(0).PaymentPlatform);
        assertNull(payment.PinpadAckCommand);
        verifyNoInteractions(pinpadPaymentCreateService);
        verify(pinpadConfigurationSearchService, never()).connection(any(), any(), any());
    }

    @Test
    void cannotBypassIntegratedApprovalByClaimingManualTerminal() {
        var payment = PinpadPaymentCreateServiceTest.payment();
        payment.PaymentPlatform = "POS_MANUAL"; payment.PinpadPaymentId = null;
        assertThrows(TrxPaymentBuildException.class, () -> service.save(payment));
        verifyNoInteractions(pinpadPaymentCreateService, trxPaymentRepository);
    }

    @Test
    void disabledIntegrationRejectsConnectedRequestAndCannotConvertProofToManual() {
        when(pinpadConfigurationSearchService.isIntegrationEnabled()).thenReturn(false);
        var payment = PinpadPaymentCreateServiceTest.payment();
        assertThrows(TrxPaymentBuildException.class, () -> service.preparePinpad(payment));
        assertThrows(TrxPaymentBuildException.class, () -> service.save(payment));
        payment.PaymentPlatform = "POS_MANUAL"; payment.PinpadPaymentId = null;
        payment.PinpadResult = new com.ccadmin.app.payment.model.dto.PinpadSignedMessageDto("proof", "signature");
        assertThrows(TrxPaymentBuildException.class, () -> service.save(payment));
        verifyNoInteractions(pinpadPaymentCreateService, trxPaymentRepository);
    }
}
