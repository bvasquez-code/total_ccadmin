package com.ccadmin.app.sale.service;

import com.ccadmin.app.shared.service.IGenericTaskService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreditNoteSunatEmissionTaskService implements IGenericTaskService {

    private final CreditNoteSunatEmissionService creditNoteSunatEmissionService;
    private final String creditNoteCod;

    public CreditNoteSunatEmissionTaskService(CreditNoteSunatEmissionService creditNoteSunatEmissionService, String creditNoteCod) {
        this.creditNoteSunatEmissionService = creditNoteSunatEmissionService;
        this.creditNoteCod = creditNoteCod;
    }

    @Override
    public void execute() {
        try {
            this.creditNoteSunatEmissionService.emitCreditNote(this.creditNoteCod);
        } catch (Exception ex) {
            log.error("Error en emision SUNAT de nota de credito {}: {}", this.creditNoteCod, ex.getMessage(), ex);
        }
    }
}
