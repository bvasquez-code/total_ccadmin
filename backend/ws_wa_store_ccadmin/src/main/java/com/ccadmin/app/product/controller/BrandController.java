package com.ccadmin.app.product.controller;

import com.ccadmin.app.product.model.dto.BrandRegisterMassiveDto;
import com.ccadmin.app.product.model.entity.BrandEntity;
import com.ccadmin.app.product.service.BrandService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/brand")
public class BrandController {

    @Autowired
    private BrandService brandService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String brandCod) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.brandService.findById(brandCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, int Page) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.brandService.findAll(Query, Page)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam String BrandCod) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    this.brandService.findDataForm(BrandCod), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody BrandEntity brand) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.brandService.save(brand)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("saveAll")
    public ResponseEntity<ResponseWsDto> saveAll(@RequestBody BrandRegisterMassiveDto brandRegisterMassive) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    this.brandService.saveAll(brandRegisterMassive), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

}
