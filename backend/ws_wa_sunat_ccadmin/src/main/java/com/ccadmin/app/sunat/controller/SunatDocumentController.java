package com.ccadmin.app.sunat.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.sunat.model.dto.SunatDocumentRegisterDto;
import com.ccadmin.app.sunat.model.dto.SunatElectronicDocumentDto;
import com.ccadmin.app.sunat.service.SunatDocumentCreateService;
import com.ccadmin.app.sunat.service.SunatDocumentOperationService;
import com.ccadmin.app.sunat.service.SunatDocumentSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/sunat/document")
public class SunatDocumentController {

    @Autowired
    private SunatDocumentSearchService sunatDocumentSearchService;

    @Autowired
    private SunatDocumentCreateService sunatDocumentCreateService;

    @Autowired
    private SunatDocumentOperationService sunatDocumentOperationService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String SunatDocumentCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentSearchService.findById(SunatDocumentCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, @RequestParam int Page) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentSearchService.findAll(Query, Page)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findStatus")
    public ResponseEntity<ResponseWsDto> findStatus(@RequestParam String SunatDocumentCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentSearchService.findStatus(SunatDocumentCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findFiles")
    public ResponseEntity<ResponseWsDto> findFiles(@RequestParam String SunatDocumentCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentSearchService.findFiles(SunatDocumentCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findAttempts")
    public ResponseEntity<ResponseWsDto> findAttempts(@RequestParam String SunatDocumentCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentSearchService.findAttempts(SunatDocumentCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findResponse")
    public ResponseEntity<ResponseWsDto> findResponse(@RequestParam String SunatDocumentCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentSearchService.findByIdOrThrow(SunatDocumentCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("register")
    public ResponseEntity<ResponseWsDto> register(@RequestBody SunatDocumentRegisterDto request) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentCreateService.register(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("generateXml")
    public ResponseEntity<ResponseWsDto> generateXml(@RequestBody SunatElectronicDocumentDto request) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentOperationService.generateXml(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("generateXmlById")
    public ResponseEntity<ResponseWsDto> generateXmlById(@RequestParam String SunatDocumentCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentOperationService.generateXml(SunatDocumentCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("signXml")
    public ResponseEntity<ResponseWsDto> signXml(@RequestParam String SunatDocumentCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentOperationService.signXml(SunatDocumentCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("generateZip")
    public ResponseEntity<ResponseWsDto> generateZip(@RequestParam String SunatDocumentCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentOperationService.generateZip(SunatDocumentCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("send")
    public ResponseEntity<ResponseWsDto> send(@RequestParam String SunatDocumentCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentOperationService.send(SunatDocumentCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("consultTicket")
    public ResponseEntity<ResponseWsDto> consultTicket(@RequestParam String SunatDocumentCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentOperationService.consultTicket(SunatDocumentCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("retry")
    public ResponseEntity<ResponseWsDto> retry(@RequestParam String SunatDocumentCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatDocumentOperationService.retry(SunatDocumentCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }
}
