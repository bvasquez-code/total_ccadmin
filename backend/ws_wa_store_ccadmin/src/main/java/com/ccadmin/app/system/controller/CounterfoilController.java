package com.ccadmin.app.system.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.system.model.dto.CounterfoilRegisterDto;
import com.ccadmin.app.system.model.dto.CounterfoilRegisterListDto;
import com.ccadmin.app.system.model.entity.CounterfoilEntity;
import com.ccadmin.app.system.service.CounterfoilCreateService;
import com.ccadmin.app.system.service.CounterfoilSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/counterfoil")
public class CounterfoilController {

    @Autowired
    private CounterfoilSearchService counterfoilSearchService;
    @Autowired
    private CounterfoilCreateService counterfoilCreateService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String CounterfoilCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(counterfoilSearchService.findById(CounterfoilCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("existsSeries")
    public ResponseEntity<ResponseWsDto> existsSeries(@RequestParam String Series) {
        try { return new ResponseEntity<>(new ResponseWsDto(counterfoilSearchService.existsSeries(Series)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, @RequestParam int Page, @RequestParam String StoreCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(counterfoilSearchService.findAll(Query, Page,StoreCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findActives")
    public ResponseEntity<ResponseWsDto> findActives() {
        try { return new ResponseEntity<>(new ResponseWsDto(counterfoilSearchService.findActives()), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("formData")
    public ResponseEntity<ResponseWsDto> formData(@RequestParam(required = false) String CounterfoilCod) {
        try { return new ResponseEntity<>(counterfoilSearchService.findDataForm(CounterfoilCod), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody CounterfoilRegisterDto counterfoil) {
        try { return new ResponseEntity<>(new ResponseWsDto(counterfoilCreateService.save(counterfoil)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("saveAll")
    public ResponseEntity<ResponseWsDto> saveAll(@RequestBody CounterfoilRegisterListDto counterfoilRegisterList) {
        try { return new ResponseEntity<>(new ResponseWsDto(counterfoilCreateService.saveAll(counterfoilRegisterList)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("enable")
    public ResponseEntity<ResponseWsDto> enable(@RequestBody CounterfoilEntity request) {
        try { return new ResponseEntity<>(new ResponseWsDto(counterfoilCreateService.enable(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("disable")
    public ResponseEntity<ResponseWsDto> disable(@RequestBody CounterfoilEntity request) {
        try { return new ResponseEntity<>(new ResponseWsDto(counterfoilCreateService.disable(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    /** Relación simple: attach/detach */
    @PostMapping("attachStore")
    public ResponseEntity<ResponseWsDto> attachStore(@RequestParam String CounterfoilCod, @RequestParam String StoreCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(counterfoilCreateService.attachStore(CounterfoilCod, StoreCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("detachStore")
    public ResponseEntity<ResponseWsDto> detachStore(@RequestParam String CounterfoilCod, @RequestParam String StoreCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(counterfoilCreateService.detachStore(CounterfoilCod, StoreCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }
}
