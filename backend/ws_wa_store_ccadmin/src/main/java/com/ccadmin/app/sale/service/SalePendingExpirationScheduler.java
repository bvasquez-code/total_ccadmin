package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class SalePendingExpirationScheduler {

    private static final String SYSTEM_USER = "SYSTEM";

    @Autowired
    private SaleHeadRepository saleHeadRepository;
    @Autowired
    private ExpiredSaleCancellationService expiredSaleCancellationService;
    @Value("${app.sale.pending-expiration-minutes:60}")
    private long expirationMinutes;

    @Scheduled(fixedDelayString = "${app.sale.pending-expiration-scan-ms:60000}")
    public void cancelExpiredPendingSales() {
        Date expirationLimit = new Date(
                System.currentTimeMillis() - this.expirationMinutes * 60_000L
        );
        List<SaleHeadEntity> expiredSales =
                this.saleHeadRepository.findExpiredPendingSales(expirationLimit);

        for (SaleHeadEntity sale : expiredSales) {
            try {
                boolean cancelled = this.expiredSaleCancellationService.cancelExpiredSale(
                        sale.SaleCod,
                        expirationLimit,
                        SYSTEM_USER
                );
                if (cancelled) {
                    log.info("VENTA_PENDIENTE_CANCELADA -->> {}", sale.SaleCod);
                }
            } catch (Exception exception) {
                log.error(
                        "ERROR_CANCELACION_VENTA_PENDIENTE -->> {} : {}",
                        sale.SaleCod,
                        exception.getMessage(),
                        exception
                );
            }
        }
    }
}
