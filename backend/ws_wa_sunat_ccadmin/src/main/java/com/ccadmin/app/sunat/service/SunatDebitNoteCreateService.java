package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.dto.SunatDebitNoteProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.SunatSendResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SunatDebitNoteCreateService {

    @Autowired
    private SunatDocumentOperationService sunatDocumentOperationService;

    public SunatSendResultDto processDebitNote(SunatDebitNoteProcessRequestDto request) {
        return this.sunatDocumentOperationService.process(request.toElectronicDocumentDto());
    }
}
