package com.ccadmin.app.sunat.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.sunat.model.dto.SunatCreditNoteProcessRequestDto;
import com.ccadmin.app.sunat.service.SunatCreditNoteCreateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/sunat/creditNote")
public class SunatCreditNoteController {

    @Autowired
    private SunatCreditNoteCreateService sunatCreditNoteCreateService;

    @PostMapping("process")
    public ResponseEntity<ResponseWsDto> process(@RequestBody SunatCreditNoteProcessRequestDto request) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatCreditNoteCreateService.processCreditNote(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }
}
