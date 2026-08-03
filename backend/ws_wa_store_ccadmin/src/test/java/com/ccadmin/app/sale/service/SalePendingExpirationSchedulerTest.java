package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
import com.ccadmin.app.system.model.dto.IndicatorDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalePendingExpirationSchedulerTest {

    @Mock
    private SaleHeadRepository saleHeadRepository;
    @Mock
    private ExpiredSaleCancellationService expiredSaleCancellationService;
    @Mock
    private CatalogSearchShared catalogSearchShared;
    @InjectMocks
    private SalePendingExpirationScheduler scheduler;

    @Test
    void skipsProcessWhenAutomaticCancellationIsDisabled() {
        when(catalogSearchShared.isIndicatorSystemEnabled(
                BusinessConfigConstants.ConfigCod.IND_CANCEL_PENDING_AUTOMATIC_SALE
        )).thenReturn(false);

        scheduler.cancelExpiredPendingSales();

        verify(catalogSearchShared, never()).findConfigSystem(
                BusinessConfigConstants.ConfigCod.CANCEL_PENDING_AUTOMATIC_SALE_TIME
        );
        verify(saleHeadRepository, never()).findExpiredPendingSales(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void usesConfiguredExpirationMilliseconds() throws Exception {
        SaleHeadEntity sale = new SaleHeadEntity();
        sale.SaleCod = "ST001";
        when(catalogSearchShared.isIndicatorSystemEnabled(
                BusinessConfigConstants.ConfigCod.IND_CANCEL_PENDING_AUTOMATIC_SALE
        )).thenReturn(true);
        when(catalogSearchShared.findConfigSystem(
                BusinessConfigConstants.ConfigCod.CANCEL_PENDING_AUTOMATIC_SALE_TIME
        )).thenReturn(new IndicatorDto(
                BusinessConfigConstants.ConfigCod.CANCEL_PENDING_AUTOMATIC_SALE_TIME,
                "1800000"
        ));
        when(saleHeadRepository.findExpiredPendingSales(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(sale));
        when(expiredSaleCancellationService.cancelExpiredSale(
                org.mockito.ArgumentMatchers.eq(sale.SaleCod),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("SYSTEM")
        )).thenReturn(true);

        long beforeExecution = System.currentTimeMillis();
        scheduler.cancelExpiredPendingSales();
        long afterExecution = System.currentTimeMillis();

        ArgumentCaptor<Date> expirationLimitCaptor = ArgumentCaptor.forClass(Date.class);
        verify(saleHeadRepository).findExpiredPendingSales(expirationLimitCaptor.capture());
        long configuredDelay = 1_800_000L;
        long expirationLimitMillis = expirationLimitCaptor.getValue().getTime();
        assertTrue(expirationLimitMillis >= beforeExecution - configuredDelay);
        assertTrue(expirationLimitMillis <= afterExecution - configuredDelay);
        verify(expiredSaleCancellationService).cancelExpiredSale(
                org.mockito.ArgumentMatchers.eq(sale.SaleCod),
                org.mockito.ArgumentMatchers.same(expirationLimitCaptor.getValue()),
                org.mockito.ArgumentMatchers.eq("SYSTEM")
        );
    }

    @Test
    void skipsProcessWhenConfiguredTimeIsNotNumeric() {
        when(catalogSearchShared.isIndicatorSystemEnabled(
                BusinessConfigConstants.ConfigCod.IND_CANCEL_PENDING_AUTOMATIC_SALE
        )).thenReturn(true);
        when(catalogSearchShared.findConfigSystem(
                BusinessConfigConstants.ConfigCod.CANCEL_PENDING_AUTOMATIC_SALE_TIME
        )).thenReturn(new IndicatorDto(
                BusinessConfigConstants.ConfigCod.CANCEL_PENDING_AUTOMATIC_SALE_TIME,
                "abc"
        ));

        scheduler.cancelExpiredPendingSales();

        verify(saleHeadRepository, never()).findExpiredPendingSales(org.mockito.ArgumentMatchers.any());
    }
}
