package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.dto.SunatDespatchAdviceProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.SunatSendResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SunatDespatchAdviceCreateService {

    @Autowired
    private SunatDocumentOperationService sunatDocumentOperationService;

    public SunatSendResultDto processDespatchAdvice(SunatDespatchAdviceProcessRequestDto request) {
        return this.sunatDocumentOperationService.process(request.toElectronicDocumentDto());
    }
}
