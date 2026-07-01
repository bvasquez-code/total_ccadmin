package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.dto.CreditNoteDetailDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatElectronicDocumentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CreditNoteSunatEmissionService {

    @Autowired
    private CreditNoteSearchService creditNoteSearchService;

    @Autowired
    private CreditNoteSunatPayloadBuildService creditNoteSunatPayloadBuildService;

    @Autowired
    private SaleSunatClientService saleSunatClientService;

    public void emitCreditNote(String creditNoteCod) {
        log.info("INI - EMISION SUNAT NOTA CREDITO : {}", creditNoteCod);
        CreditNoteDetailDto creditNoteDetail = this.creditNoteSearchService.findById(creditNoteCod);
        SunatElectronicDocumentDto request = this.creditNoteSunatPayloadBuildService.build(creditNoteDetail);
        Object response = this.saleSunatClientService.process(request).Data;
        log.info("FIN - EMISION SUNAT NOTA CREDITO : {} -> {}", creditNoteCod, response);
    }
}
