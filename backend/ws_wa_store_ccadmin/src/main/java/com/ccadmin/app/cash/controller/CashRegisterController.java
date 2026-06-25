package com.ccadmin.app.cash.controller;

import com.ccadmin.app.cash.model.entity.CashRegisterEntity;
import com.ccadmin.app.cash.service.CashRegisterCreateService;
import com.ccadmin.app.cash.service.CashRegisterSearchService;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/cash/register")
public class CashRegisterController {

    @Autowired
    private CashRegisterSearchService searchService;

    @Autowired
    private CashRegisterCreateService createService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String RegisterCod) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(searchService.findById(RegisterCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, @RequestParam int Page) {
        try {
            ResponsePageSearchT<CashRegisterEntity> data = searchService.findAll(Query, Page);
            return new ResponseEntity<>(new ResponseWsDto(data), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findActives")
    public ResponseEntity<ResponseWsDto> findActives() {
        try {
            List<CashRegisterEntity> list = searchService.findActives();
            return new ResponseEntity<>(new ResponseWsDto(list), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findActivesByStore")
    public ResponseEntity<ResponseWsDto> findActivesByStore(@RequestParam String StoreCod) {
        try {
            List<CashRegisterEntity> list = searchService.findActivesByStore(StoreCod);
            return new ResponseEntity<>(new ResponseWsDto(list), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody CashRegisterEntity entity) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(createService.save(entity)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("saveAll")
    public ResponseEntity<ResponseWsDto> saveAll(@RequestBody List<CashRegisterEntity> list) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(createService.saveAll(list)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("enable")
    public ResponseEntity<ResponseWsDto> enable(@RequestParam String RegisterCod) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(createService.enable(RegisterCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("disable")
    public ResponseEntity<ResponseWsDto> disable(@RequestParam String RegisterCod) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(createService.disable(RegisterCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam String RegisterCod) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(searchService.findDataForm(RegisterCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
