package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.dto.SunatCreditNoteProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.SunatSendResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SunatCreditNoteCreateService {

    @Autowired
    private SunatDocumentOperationService sunatDocumentOperationService;

    public SunatSendResultDto processCreditNote(SunatCreditNoteProcessRequestDto request) {
        return this.sunatDocumentOperationService.process(request.toElectronicDocumentDto());
    }
}
