package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.dto.SunatInvoiceProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.SunatSendResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SunatInvoiceCreateService {

    @Autowired
    private SunatDocumentOperationService sunatDocumentOperationService;

    public SunatSendResultDto processInvoice(SunatInvoiceProcessRequestDto request) {
        return this.sunatDocumentOperationService.process(request.toElectronicDocumentDto());
    }
}
