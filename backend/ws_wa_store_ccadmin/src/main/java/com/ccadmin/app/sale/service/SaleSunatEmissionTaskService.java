package com.ccadmin.app.sale.service;

import com.ccadmin.app.shared.service.IGenericTaskService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SaleSunatEmissionTaskService implements IGenericTaskService {

    private final SaleSunatEmissionService saleSunatEmissionService;
    private final String saleCod;

    public SaleSunatEmissionTaskService(SaleSunatEmissionService saleSunatEmissionService, String saleCod) {
        this.saleSunatEmissionService = saleSunatEmissionService;
        this.saleCod = saleCod;
    }

    @Override
    public void execute() {
        try {
            this.saleSunatEmissionService.emitSale(this.saleCod);
        } catch (Exception ex) {
            log.error("Error en emision SUNAT de venta {}: {}", this.saleCod, ex.getMessage(), ex);
        }
    }
}
