package com.ccadmin.app.cash.service;

import com.ccadmin.app.cash.model.dto.CurrentCashSessionDto;
import com.ccadmin.app.cash.model.entity.CashRegisterEntity;
import com.ccadmin.app.cash.model.entity.CashSessionEntity;
import com.ccadmin.app.cash.repository.CashRegisterRepository;
import com.ccadmin.app.cash.repository.CashSessionRepository;
import com.ccadmin.app.sale.model.idto.IExpectedTotalsDto;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashSessionAdminServiceTest {

    @Mock
    private CashRegisterRepository cashRegisterRepository;
    @Mock
    private CashSessionRepository cashSessionRepository;
    @Mock
    private SaleHeadRepository saleHeadRepository;

    private CurrentUserCashSessionAdminService service;
    private CashRegisterEntity currentRegister;

    @BeforeEach
    void setUp() {
        service = new CurrentUserCashSessionAdminService();
        ReflectionTestUtils.setField(service, "cashRegisterRepository", cashRegisterRepository);
        ReflectionTestUtils.setField(service, "sessionRepository", cashSessionRepository);
        ReflectionTestUtils.setField(service, "saleHeadRepository", saleHeadRepository);

        currentRegister = new CashRegisterEntity();
        currentRegister.RegisterCod = "00000001";
        currentRegister.StoreCod = "T001";
        currentRegister.UserCod = "USER01";
        currentRegister.Name = "Caja USER01";

    }

    @Test
    void findsCurrentRegisterAndReportsClosedStateWhenThereIsNoOpenSession() {
        when(cashRegisterRepository.findActiveByUserAndStore("USER01", "T001"))
                .thenReturn(Optional.of(currentRegister));
        CurrentCashSessionDto current = service.findCurrent();

        assertSame(currentRegister, current.CashRegister);
        assertFalse(current.IsOpen);
        assertNull(current.CashSession);
    }

    @Test
    void restoresTheOpenCashSessionIntoTheNewApplicationSession() {
        CashSessionEntity openSession = new CashSessionEntity();
        openSession.CashSessionID = 7L;
        openSession.RegisterCod = "00000001";
        openSession.StoreCod = "T001";
        openSession.UserCod = "USER01";
        openSession.IsOpen = 1;
        openSession.SessionStatus = 'O';

        when(cashSessionRepository.findOpenIdByUserAndStore("USER01", "T001"))
                .thenReturn(Optional.of(7L));
        when(cashSessionRepository.findByCashSessionId(7L)).thenReturn(Optional.of(openSession));
        when(cashRegisterRepository.findActiveByRegisterCod("00000001"))
                .thenReturn(Optional.of(currentRegister));

        CurrentCashSessionDto current = service.findCurrent();

        assertSame(openSession, current.CashSession);
        assertTrue(current.IsOpen);
        assertEquals(7L, service.getCashSessionID());
    }

    @Test
    void opensOnlyTheRegisterAssignedToTheCurrentUserAndStore() {
        when(cashRegisterRepository.findActiveByUserAndStore("USER01", "T001"))
                .thenReturn(Optional.of(currentRegister));
        when(cashSessionRepository.findOpenByRegister("00000001"))
                .thenReturn(Optional.empty());
        when(cashSessionRepository.save(any(CashSessionEntity.class)))
                .thenAnswer(invocation -> {
                    CashSessionEntity cashSession = invocation.getArgument(0);
                    cashSession.CashSessionID = 1L;
                    return cashSession;
                });

        CashSessionEntity opened = service.open("PEN", "Inicio", BigDecimal.TEN);

        assertEquals("00000001", opened.RegisterCod);
        assertEquals("T001", opened.StoreCod);
        assertEquals("USER01", opened.UserCod);
        assertEquals(1, opened.IsOpen);
        assertEquals(1L, service.getCashSessionID());
    }

    @Test
    void rejectsClosingASessionOwnedByAnotherUser() {
        CashSessionEntity foreignSession = new CashSessionEntity();
        foreignSession.CashSessionID = 9L;
        foreignSession.RegisterCod = "00000001";
        foreignSession.StoreCod = "T001";
        foreignSession.UserCod = "OTHER";
        foreignSession.IsOpen = 1;
        foreignSession.SessionStatus = 'O';
        service.setCurrentCashSessionId(9L);
        when(cashSessionRepository.findByCashSessionId(9L)).thenReturn(Optional.of(foreignSession));

        assertThrows(IllegalStateException.class, () -> service.close("N", null, null, "Cierre"));

        verify(cashSessionRepository, never()).save(any());
    }

    @Test
    void closesWithoutCashCountUsingOnlyBackendSessionContext() {
        CashSessionEntity openSession = new CashSessionEntity();
        openSession.CashSessionID = 7L;
        openSession.RegisterCod = "00000001";
        openSession.StoreCod = "T001";
        openSession.UserCod = "USER01";
        openSession.CurrencyCod = "PEN";
        openSession.OpeningFloatAmount = BigDecimal.TEN;
        openSession.IsOpen = 1;
        openSession.SessionStatus = 'O';
        service.setCurrentCashSessionId(7L);

        IExpectedTotalsDto totals = mock(IExpectedTotalsDto.class);
        when(totals.getCash()).thenReturn(BigDecimal.valueOf(25));
        when(totals.getOther()).thenReturn(BigDecimal.valueOf(15));
        when(cashSessionRepository.findByCashSessionId(7L)).thenReturn(Optional.of(openSession));
        when(saleHeadRepository.getExpectedTotalsForSession(7L)).thenReturn(totals);
        when(cashSessionRepository.save(openSession)).thenReturn(openSession);

        CashSessionEntity closed = service.close("N", null, null, "Cierre sin arqueo");

        assertEquals(BigDecimal.valueOf(35), closed.ExpectedCashAmount);
        assertEquals(BigDecimal.valueOf(15), closed.ExpectedOtherAmount);
        assertEquals("N", closed.HasCashCount);
        assertNull(closed.CountedTotalAmount);
        assertNull(closed.DifferenceAmount);
        assertEquals(0, closed.IsOpen);
        assertNull(service.getCashSessionID());
    }

    private static class CurrentUserCashSessionAdminService extends CashSessionAdminService {

        private Long currentCashSessionId;

        @Override
        public String getUserCod() {
            return "USER01";
        }

        @Override
        public String getStoreCod() {
            return "T001";
        }

        @Override
        public Long getCashSessionID() {
            return currentCashSessionId;
        }

        @Override
        protected void setCashSessionID(Long cashSessionId) {
            this.currentCashSessionId = cashSessionId;
        }

        @Override
        protected void clearCashSessionID(Long cashSessionId) {
            this.currentCashSessionId = null;
        }

        void setCurrentCashSessionId(Long cashSessionId) {
            this.currentCashSessionId = cashSessionId;
        }
    }
}
