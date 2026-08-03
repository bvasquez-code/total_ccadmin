package com.ccadmin.app.sale.service;

import com.ccadmin.app.shared.service.IGenericTaskService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SaleSunatEmissionTaskService implements IGenericTaskService {

    private final SaleSunatEmissionService saleSunatEmissionService;
    private final String saleCod;
    private final String documentCod;

    public SaleSunatEmissionTaskService(
            SaleSunatEmissionService saleSunatEmissionService,
            String saleCod,
            String documentCod
    ) {
        this.saleSunatEmissionService = saleSunatEmissionService;
        this.saleCod = saleCod;
        this.documentCod = documentCod;
    }

    @Override
    public void execute() {
        try {
            this.saleSunatEmissionService.emitSale(this.saleCod, this.documentCod);
        } catch (Exception ex) {
            log.error(
                    "Error en emision SUNAT de venta {} documento {}: {}",
                    this.saleCod,
                    this.documentCod,
                    ex.getMessage(),
                    ex
            );
        }
    }
}
