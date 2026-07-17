package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalePendingExpirationSchedulerTest {

    @Mock
    private SaleHeadRepository saleHeadRepository;
    @Mock
    private ExpiredSaleCancellationService expiredSaleCancellationService;
    @InjectMocks
    private SalePendingExpirationScheduler scheduler;

    @Test
    void shouldSendEveryExpiredCandidateToCancellationService() throws Exception {
        SaleHeadEntity sale = new SaleHeadEntity();
        sale.SaleCod = "SL001";
        ReflectionTestUtils.setField(this.scheduler, "expirationMinutes", 60L);
        when(this.saleHeadRepository.findExpiredPendingSales(any())).thenReturn(List.of(sale));
        when(this.expiredSaleCancellationService.cancelExpiredSale(
                eq("SL001"),
                any(),
                eq("SYSTEM")
        )).thenReturn(true);

        this.scheduler.cancelExpiredPendingSales();

        verify(this.expiredSaleCancellationService).cancelExpiredSale(
                eq("SL001"),
                any(),
                eq("SYSTEM")
        );
    }
}
