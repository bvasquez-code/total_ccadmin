package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
import com.ccadmin.app.system.model.dto.IndicatorDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private CatalogSearchShared catalogSearchShared;

    public void cancelExpiredPendingSales() {
        if (!this.catalogSearchShared.isIndicatorSystemEnabled(
                BusinessConfigConstants.ConfigCod.IND_CANCEL_PENDING_AUTOMATIC_SALE
        )) {
            return;
        }

        Long expirationMillis = this.findExpirationMillis();
        if (expirationMillis == null) {
            return;
        }

        Date expirationLimit;
        try {
            expirationLimit = new Date(Math.subtractExact(System.currentTimeMillis(), expirationMillis));
        } catch (ArithmeticException exception) {
            log.error(
                    "CONFIGURACION_CANCELACION_VENTA_PENDIENTE_FUERA_DE_RANGO -->> {} : {} MS",
                    BusinessConfigConstants.ConfigCod.CANCEL_PENDING_AUTOMATIC_SALE_TIME,
                    expirationMillis
            );
            return;
        }

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

    private Long findExpirationMillis() {
        IndicatorDto timeConfig = this.catalogSearchShared.findConfigSystem(
                BusinessConfigConstants.ConfigCod.CANCEL_PENDING_AUTOMATIC_SALE_TIME
        );
        String configuredValue = timeConfig == null ? null : timeConfig.Value;

        try {
            long expirationMillis = Long.parseLong(configuredValue == null ? "" : configuredValue.trim());
            if (expirationMillis < 1) {
                throw new NumberFormatException();
            }
            return expirationMillis;
        } catch (NumberFormatException exception) {
            log.error(
                    "CONFIGURACION_CANCELACION_VENTA_PENDIENTE_INVALIDA -->> {} debe ser un entero mayor que cero expresado en milisegundos: {}",
                    BusinessConfigConstants.ConfigCod.CANCEL_PENDING_AUTOMATIC_SALE_TIME,
                    configuredValue
            );
            return null;
        }
    }
}
