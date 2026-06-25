package com.ccadmin.app.system.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.service.CurrencyCreateService;
import com.ccadmin.app.system.service.CurrencySearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/currency")
public class CurrencyController {

    @Autowired
    private CurrencySearchService currencySearchService;

    @Autowired
    private CurrencyCreateService currencyCreateService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String CurrencyCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(currencySearchService.findById(CurrencyCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, @RequestParam int Page) {
        try { return new ResponseEntity<>(new ResponseWsDto(currencySearchService.findAll(Query, Page)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findActives")
    public ResponseEntity<ResponseWsDto> findActives() {
        try { return new ResponseEntity<>(new ResponseWsDto(currencySearchService.findAllActive()), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findCurrencySystem")
    public ResponseEntity<ResponseWsDto> findCurrencySystem() {
        try { return new ResponseEntity<>(new ResponseWsDto(currencySearchService.findCurrencySystem()), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam(required = false) String CurrencyCod) {
        try { return new ResponseEntity<>(currencySearchService.findDataForm(CurrencyCod), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody CurrencyEntity currency) {
        try { return new ResponseEntity<>(new ResponseWsDto(currencyCreateService.save(currency)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("saveAll")
    public ResponseEntity<ResponseWsDto> saveAll(@RequestBody List<CurrencyEntity> currencyList) {
        try { return new ResponseEntity<>(new ResponseWsDto(currencyCreateService.saveAll(currencyList)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("enable")
    public ResponseEntity<ResponseWsDto> enable(@RequestBody CurrencyEntity request) {
        try { return new ResponseEntity<>(new ResponseWsDto(currencyCreateService.enable(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("disable")
    public ResponseEntity<ResponseWsDto> disable(@RequestBody CurrencyEntity request) {
        try { return new ResponseEntity<>(new ResponseWsDto(currencyCreateService.disable(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }
}
