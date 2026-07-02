package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.dto.SunatReceiptProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.SunatSendResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SunatReceiptCreateService {

    @Autowired
    private SunatDocumentOperationService sunatDocumentOperationService;

    public SunatSendResultDto processReceipt(SunatReceiptProcessRequestDto request) {
        return this.sunatDocumentOperationService.process(request.toElectronicDocumentDto());
    }
}
