package com.ccadmin.app.product.controller;

import com.ccadmin.app.product.model.dto.ProductTaxConfigRegisterDto;
import com.ccadmin.app.product.model.entity.ProductTaxConfigEntity;
import com.ccadmin.app.product.service.ProductTaxConfigCreateService;
import com.ccadmin.app.product.service.ProductTaxConfigSearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
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
@RequestMapping("api/v1/productTaxConfig")
public class ProductTaxConfigController {

    @Autowired
    private ProductTaxConfigSearchService productTaxConfigSearchService;

    @Autowired
    private ProductTaxConfigCreateService productTaxConfigCreateService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam Long ProductTaxConfigId) {
        try { return new ResponseEntity<>(new ResponseWsDto(productTaxConfigSearchService.findById(ProductTaxConfigId)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findByProductStore")
    public ResponseEntity<ResponseWsDto> findByProductStore(@RequestParam String ProductCod, @RequestParam String StoreCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(productTaxConfigSearchService.findByProductAndStore(ProductCod, StoreCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam String ProductCod, @RequestParam String StoreCod) {
        try { return new ResponseEntity<>(productTaxConfigSearchService.findDataForm(ProductCod, StoreCod), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody ProductTaxConfigEntity config) {
        try { return new ResponseEntity<>(new ResponseWsDto(productTaxConfigCreateService.save(config)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("saveAllByProductStore")
    public ResponseEntity<ResponseWsDto> saveAllByProductStore(@RequestBody ProductTaxConfigRegisterDto request) {
        try { return new ResponseEntity<>(new ResponseWsDto(productTaxConfigCreateService.saveAllByProductStore(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("disable")
    public ResponseEntity<ResponseWsDto> disable(@RequestBody ProductTaxConfigEntity request) {
        try { return new ResponseEntity<>(new ResponseWsDto(productTaxConfigCreateService.disable(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }
}
