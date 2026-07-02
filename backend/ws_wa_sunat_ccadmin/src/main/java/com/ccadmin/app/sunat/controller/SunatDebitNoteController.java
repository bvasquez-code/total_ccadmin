package com.ccadmin.app.sunat.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.sunat.model.dto.SunatDebitNoteProcessRequestDto;
import com.ccadmin.app.sunat.service.SunatDebitNoteCreateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/sunat/debitNote")
public class SunatDebitNoteController {

    @Autowired
    private SunatDebitNoteCreateService sunatDebitNoteCreateService;

    @PostMapping("process")
    public ResponseEntity<ResponseWsDto> process(@RequestBody SunatDebitNoteProcessRequestDto request) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDebitNoteCreateService.processDebitNote(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }
}
