package com.ccadmin.app.transfer.controller;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.transfer.model.entity.CarrierEntity;
import com.ccadmin.app.transfer.service.CarrierCreateService;
import com.ccadmin.app.transfer.service.CarrierSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/transfers/carriers")
public class CarrierController {

    @Autowired
    private CarrierSearchService searchService;

    @Autowired
    private CarrierCreateService createService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String CarrierCod) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(searchService.findById(CarrierCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, @RequestParam int Page) {
        try {
            ResponsePageSearchT<CarrierEntity> data = searchService.findAll(Query, Page);
            return new ResponseEntity<>(new ResponseWsDto(data), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findActives")
    public ResponseEntity<ResponseWsDto> findActives() {
        try {
            List<CarrierEntity> list = searchService.findActives();
            return new ResponseEntity<>(new ResponseWsDto(list), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody CarrierEntity entity) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(createService.save(entity)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("saveAll")
    public ResponseEntity<ResponseWsDto> saveAll(@RequestBody List<CarrierEntity> list) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(createService.saveAll(list)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("enable")
    public ResponseEntity<ResponseWsDto> enable(@RequestParam String CarrierCod) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(createService.enable(CarrierCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("disable")
    public ResponseEntity<ResponseWsDto> disable(@RequestParam String CarrierCod) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(createService.disable(CarrierCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("{carrierCod}")
    public ResponseEntity<ResponseWsDto> delete(@PathVariable String carrierCod) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(createService.delete(carrierCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

}
