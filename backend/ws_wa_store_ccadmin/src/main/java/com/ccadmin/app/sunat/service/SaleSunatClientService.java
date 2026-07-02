package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.constants.SunatClientConstants;
import com.ccadmin.app.sunat.model.dto.sunat.SunatCreditNoteProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatDebitNoteProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatDespatchAdviceProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatInvoiceProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatReceiptProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatWsResponseDto;
import com.ccadmin.app.shared.model.dto.UrlDataDto;
import com.ccadmin.app.shared.shared.UrlSearchShared;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class SaleSunatClientService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private UrlSearchShared urlSearchShared;


    private void validateResponse(SunatWsResponseDto response, String operation) {
        if (response == null) {
            throw new IllegalArgumentException("SUNAT sin respuesta en " + operation);
        }
        if (response.ErrorStatus || !SunatClientConstants.RESPONSE_STATUS_OK.equals(response.Status)) {
            throw new IllegalArgumentException("SUNAT error en " + operation + ": " + response.Message);
        }
    }


    public SunatWsResponseDto processInvoice(SunatInvoiceProcessRequestDto request) {
        return this.postProcess(SunatClientConstants.URL_KEY_INVOICE, SunatClientConstants.MESSAGE_INACTIVE_INVOICE_URL, request);
    }

    public SunatWsResponseDto processReceipt(SunatReceiptProcessRequestDto request) {
        return this.postProcess(SunatClientConstants.URL_KEY_RECEIPT, SunatClientConstants.MESSAGE_INACTIVE_RECEIPT_URL, request);
    }

    public SunatWsResponseDto processCreditNote(SunatCreditNoteProcessRequestDto request) {
        return this.postProcess(SunatClientConstants.URL_KEY_CREDIT_NOTE, SunatClientConstants.MESSAGE_INACTIVE_CREDIT_NOTE_URL, request);
    }

    public SunatWsResponseDto processDebitNote(SunatDebitNoteProcessRequestDto request) {
        return this.postProcess(SunatClientConstants.URL_KEY_DEBIT_NOTE, SunatClientConstants.MESSAGE_INACTIVE_DEBIT_NOTE_URL, request);
    }

    public SunatWsResponseDto processDespatchAdvice(SunatDespatchAdviceProcessRequestDto request) {
        return this.postProcess(SunatClientConstants.URL_KEY_DESPATCH_ADVICE, SunatClientConstants.MESSAGE_INACTIVE_DESPATCH_ADVICE_URL, request);
    }

    private SunatWsResponseDto postProcess(String urlKey, String inactiveMessage, Object request) {
        UrlDataDto urlData = this.urlSearchShared.findUrlDtaSunat(urlKey);
        if (!SunatClientConstants.URL_STATUS_ACTIVE.equals(urlData.status)) {
            return SunatWsResponseDto.alert(inactiveMessage);
        }

        String url = urlData.urlAddress;
        ResponseEntity<SunatWsResponseDto> response = this.restTemplate.postForEntity(url, request, SunatWsResponseDto.class);
        this.validateResponse(response.getBody(), SunatClientConstants.OPERATION_PROCESS);
        return response.getBody();
    }

}
