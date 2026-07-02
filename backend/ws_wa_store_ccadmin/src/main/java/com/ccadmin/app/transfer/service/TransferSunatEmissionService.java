package com.ccadmin.app.transfer.service;

import com.ccadmin.app.sunat.model.dto.sunat.SunatDespatchAdviceProcessRequestDto;
import com.ccadmin.app.sunat.service.SaleSunatClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransferSunatEmissionService {

    @Autowired
    private TransferSunatPayloadBuildService transferSunatPayloadBuildService;

    @Autowired
    private SaleSunatClientService saleSunatClientService;

    public void emitTransferGuide(String transferCod) throws Exception {
        log.info("INI - EMISION SUNAT GUIA TRANSFERENCIA : {}", transferCod);
        SunatDespatchAdviceProcessRequestDto request = this.transferSunatPayloadBuildService.build(transferCod);
        Object response = this.saleSunatClientService.processDespatchAdvice(request).Data;
        log.info("FIN - EMISION SUNAT GUIA TRANSFERENCIA : {} -> {}", transferCod, response);
    }
}
