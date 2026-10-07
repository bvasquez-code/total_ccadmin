package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.exception.TrxPaymentBuildException;
import com.ccadmin.app.payment.repository.TrxPaymentRepository;
import com.ccadmin.app.shared.model.dto.SessionDto;
import com.ccadmin.app.user.shared.UserStoreShared;
import com.ccadmin.app.user.model.entity.UserStoreEntity;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.shared.CurrencyShared;
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
    @Mock UserStoreShared userStoreShared;
    @Mock PinpadPaymentCreateService pinpadPaymentCreateService;
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
}
