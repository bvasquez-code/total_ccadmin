package com.ccadmin.app.cash.service;

import com.ccadmin.app.cash.model.dto.CurrentCashSessionDto;
import com.ccadmin.app.cash.model.entity.CashRegisterEntity;
import com.ccadmin.app.cash.model.entity.CashSessionEntity;
import com.ccadmin.app.cash.repository.CashRegisterRepository;
import com.ccadmin.app.cash.repository.CashSessionItemRepository;
import com.ccadmin.app.cash.repository.CashSessionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashSessionAdminServiceTest {

    @Mock
    private CashRegisterRepository cashRegisterRepository;
    @Mock
    private CashSessionRepository cashSessionRepository;
    @Mock
    private CashSessionItemRepository cashSessionItemRepository;
    @Mock
    private SaleHeadRepository saleHeadRepository;

    private CashSessionAdminService service;
    private CashRegisterEntity currentRegister;

    @BeforeEach
    void setUp() {
        service = new CurrentUserCashSessionAdminService();
        ReflectionTestUtils.setField(service, "cashRegisterRepository", cashRegisterRepository);
        ReflectionTestUtils.setField(service, "sessionRepository", cashSessionRepository);
        ReflectionTestUtils.setField(service, "itemRepository", cashSessionItemRepository);
        ReflectionTestUtils.setField(service, "saleHeadRepository", saleHeadRepository);

        currentRegister = new CashRegisterEntity();
        currentRegister.RegisterCod = "00000001";
        currentRegister.StoreCod = "T001";
        currentRegister.UserCod = "USER01";
        currentRegister.Name = "Caja USER01";

        when(cashRegisterRepository.findActiveByUserAndStore("USER01", "T001"))
                .thenReturn(Optional.of(currentRegister));
    }

    @Test
    void findsCurrentRegisterAndReportsClosedStateWhenThereIsNoOpenSession() {
        when(cashSessionRepository.findOpenByRegister("00000001"))
                .thenReturn(Optional.empty());

        CurrentCashSessionDto current = service.findCurrent();

        assertSame(currentRegister, current.CashRegister);
        assertFalse(current.IsOpen);
        assertNull(current.CashSession);
    }

    @Test
    void opensOnlyTheRegisterAssignedToTheCurrentUserAndStore() {
        when(cashSessionRepository.findOpenByRegister("00000001"))
                .thenReturn(Optional.empty());
        when(cashSessionRepository.save(any(CashSessionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CashSessionEntity opened = service.open("", "", "PEN", "Inicio", BigDecimal.TEN);

        assertEquals("00000001", opened.RegisterCod);
        assertEquals("T001", opened.StoreCod);
        assertEquals("USER01", opened.UserCod);
        assertEquals(1, opened.IsOpen);
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
        when(cashSessionRepository.findByCashSessionId(9L)).thenReturn(Optional.of(foreignSession));

        assertThrows(IllegalStateException.class, () -> service.close(9L, "Cierre"));

        verify(cashSessionItemRepository, never()).sumNetMovements(any());
        verify(cashSessionRepository, never()).save(any());
    }

    private static class CurrentUserCashSessionAdminService extends CashSessionAdminService {

        @Override
        public String getUserCod() {
            return "USER01";
        }

        @Override
        public String getStoreCod() {
            return "T001";
        }
    }
}
