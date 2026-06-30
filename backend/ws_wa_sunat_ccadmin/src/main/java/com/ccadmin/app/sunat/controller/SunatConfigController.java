package com.ccadmin.app.sunat.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import com.ccadmin.app.sunat.service.SunatConfigCreateService;
import com.ccadmin.app.sunat.service.SunatConfigSearchService;
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
@RequestMapping("api/v1/sunat/config")
public class SunatConfigController {

    @Autowired
    private SunatConfigSearchService sunatConfigSearchService;

    @Autowired
    private SunatConfigCreateService sunatConfigCreateService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String SunatConfigCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatConfigSearchService.findById(SunatConfigCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, @RequestParam int Page) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatConfigSearchService.findAll(Query, Page)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findActive")
    public ResponseEntity<ResponseWsDto> findActive() {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatConfigSearchService.findActive()), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam(required = false) String SunatConfigCod) {
        try { return new ResponseEntity<>(sunatConfigSearchService.findDataForm(SunatConfigCod), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody SunatConfigEntity sunatConfig) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatConfigCreateService.save(sunatConfig)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("activate")
    public ResponseEntity<ResponseWsDto> activate(@RequestBody SunatConfigEntity request) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatConfigCreateService.activate(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("enable")
    public ResponseEntity<ResponseWsDto> enable(@RequestBody SunatConfigEntity request) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatConfigCreateService.enable(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("disable")
    public ResponseEntity<ResponseWsDto> disable(@RequestBody SunatConfigEntity request) {
        try { return new ResponseEntity<>(new ResponseWsDto(sunatConfigCreateService.disable(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }
}
