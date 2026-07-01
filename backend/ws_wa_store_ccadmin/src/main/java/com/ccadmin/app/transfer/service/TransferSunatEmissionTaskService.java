package com.ccadmin.app.transfer.service;

import com.ccadmin.app.shared.service.IGenericTaskService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransferSunatEmissionTaskService implements IGenericTaskService {

    private final TransferSunatEmissionService transferSunatEmissionService;
    private final String transferCod;

    public TransferSunatEmissionTaskService(TransferSunatEmissionService transferSunatEmissionService, String transferCod) {
        this.transferSunatEmissionService = transferSunatEmissionService;
        this.transferCod = transferCod;
    }

    @Override
    public void execute() {
        try {
            this.transferSunatEmissionService.emitTransferGuide(this.transferCod);
        } catch (Exception ex) {
            log.error("Error en emision SUNAT de guia de transferencia {}: {}", this.transferCod, ex.getMessage(), ex);
        }
    }
}
