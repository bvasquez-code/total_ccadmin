package com.ccadmin.app.store.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.store.model.entity.CompanyEntity;
import com.ccadmin.app.store.shared.CompanyShared;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/company")
public class CompanyController {

    private final CompanyShared companyShared;

    public CompanyController(CompanyShared companyShared) {
        this.companyShared = companyShared;
    }

    @GetMapping("find")
    public ResponseEntity<ResponseWsDto> find() {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(companyShared.findOnlyCompany()),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody CompanyEntity company) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(companyShared.saveOnlyCompany(company)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
