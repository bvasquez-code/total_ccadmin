package com.ccadmin.app.store.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.store.model.dto.StoreVirtualConfigRegisterDto;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.service.StoreService;
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
@RequestMapping("api/v1/store")
public class StoreController {

    @Autowired
    private StoreService storeService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String StoreCod) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.storeService.findById(StoreCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll( @RequestParam String Query, @RequestParam int Page) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.storeService.findAll(Query, Page)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findAllList")
    public ResponseEntity<ResponseWsDto> findAllList() {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.storeService.findAll()), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findCompanies")
    public ResponseEntity<ResponseWsDto> findCompanies() {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.storeService.findCompanies()), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findCountries")
    public ResponseEntity<ResponseWsDto> findCountries() {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.storeService.findCountries()), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDepartments")
    public ResponseEntity<ResponseWsDto> findDepartments() {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.storeService.findDepartments()), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findProvinces")
    public ResponseEntity<ResponseWsDto> findProvinces(
            @RequestParam String DepartmentCod
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.storeService.findProvinces(DepartmentCod)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDistricts")
    public ResponseEntity<ResponseWsDto> findDistricts(
            @RequestParam String ProvinceCod
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.storeService.findDistricts(ProvinceCod)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findUbigeo")
    public ResponseEntity<ResponseWsDto> findUbigeo(@RequestParam String UbigeoCod) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.storeService.findUbigeo(UbigeoCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findStoreInfo")
    public ResponseEntity<ResponseWsDto> findStoreInfo(@RequestParam String StoreCod) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.storeService.findStoreInfo(StoreCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findVirtualConfig")
    public ResponseEntity<ResponseWsDto> findVirtualConfig(@RequestParam String StoreCod) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.storeService.findVirtualConfig(StoreCod)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("initializeStoreAutomation")
    public ResponseEntity<ResponseWsDto> initializeStoreAutomation(@RequestBody StoreEntity store) {
        try {
            this.storeService.initializeStoreAutomation(store);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto("operation performed successfully"), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody StoreEntity store) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.storeService.save(store)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("saveVirtualConfig")
    public ResponseEntity<ResponseWsDto> saveVirtualConfig(@RequestBody StoreVirtualConfigRegisterDto register) {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.storeService.saveVirtualConfig(register)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
