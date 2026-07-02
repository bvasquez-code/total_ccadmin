package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.dto.sunat.SunatElectronicDocumentDto;
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
        if (response.ErrorStatus || !"200".equals(response.Status)) {
            throw new IllegalArgumentException("SUNAT error en " + operation + ": " + response.Message);
        }
    }


    public SunatWsResponseDto processInvoice(SunatElectronicDocumentDto request) {
        UrlDataDto urlData = this.urlSearchShared.findUrlDtaSunat("01_invoice");

        if(!urlData.status.equals("A")){
            return SunatWsResponseDto.alert("Url processInvoice inactiva");
        }

        String url = urlData.urlAddress;
        ResponseEntity<SunatWsResponseDto> response = this.restTemplate.postForEntity(url, request, SunatWsResponseDto.class);
        this.validateResponse(response.getBody(), "process");
        return response.getBody();
    }

    public SunatWsResponseDto processReceipt(SunatElectronicDocumentDto request) {
        UrlDataDto urlData = this.urlSearchShared.findUrlDtaSunat("03_receipt");

        if(!urlData.status.equals("A")){
            return SunatWsResponseDto.alert("Url processReceipt inactiva");
        }

        String url = urlData.urlAddress;
        ResponseEntity<SunatWsResponseDto> response = this.restTemplate.postForEntity(url, request, SunatWsResponseDto.class);
        this.validateResponse(response.getBody(), "process");
        return response.getBody();
    }

    public SunatWsResponseDto processCreditNote(SunatElectronicDocumentDto request) {
        UrlDataDto urlData = this.urlSearchShared.findUrlDtaSunat("07_creditNote");

        if(!urlData.status.equals("A")){
            return SunatWsResponseDto.alert("Url processCreditNote inactiva");
        }

        String url = urlData.urlAddress;
        ResponseEntity<SunatWsResponseDto> response = this.restTemplate.postForEntity(url, request, SunatWsResponseDto.class);
        this.validateResponse(response.getBody(), "process");
        return response.getBody();
    }

    public SunatWsResponseDto processDebitNote(SunatElectronicDocumentDto request) {
        UrlDataDto urlData = this.urlSearchShared.findUrlDtaSunat("08_debitNote");

        if(!urlData.status.equals("A")){
            return SunatWsResponseDto.alert("Url processDebitNote inactiva");
        }

        String url = urlData.urlAddress;
        ResponseEntity<SunatWsResponseDto> response = this.restTemplate.postForEntity(url, request, SunatWsResponseDto.class);
        this.validateResponse(response.getBody(), "process");
        return response.getBody();
    }

    public SunatWsResponseDto processDespatchAdvice(SunatElectronicDocumentDto request) {
        UrlDataDto urlData = this.urlSearchShared.findUrlDtaSunat("09_despatchAdvice");

        if(!urlData.status.equals("A")){
            return SunatWsResponseDto.alert("Url processDespatchAdvice inactiva");
        }

        String url = urlData.urlAddress;
        ResponseEntity<SunatWsResponseDto> response = this.restTemplate.postForEntity(url, request, SunatWsResponseDto.class);
        this.validateResponse(response.getBody(), "process");
        return response.getBody();
    }

}
