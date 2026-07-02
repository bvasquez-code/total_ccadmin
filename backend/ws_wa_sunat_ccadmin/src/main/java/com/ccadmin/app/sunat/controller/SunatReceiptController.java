package com.ccadmin.app.sunat.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.sunat.model.dto.SunatReceiptProcessRequestDto;
import com.ccadmin.app.sunat.service.SunatReceiptCreateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/sunat/receipt")
public class SunatReceiptController {

    @Autowired
    private SunatReceiptCreateService sunatReceiptCreateService;

    @PostMapping("process")
    public ResponseEntity<ResponseWsDto> process(@RequestBody SunatReceiptProcessRequestDto request) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatReceiptCreateService.processReceipt(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }
}
