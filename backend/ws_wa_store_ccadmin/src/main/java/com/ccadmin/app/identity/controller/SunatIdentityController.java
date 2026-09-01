package com.ccadmin.app.identity.controller;

import com.ccadmin.app.identity.service.SunatIdentitySearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/sunatIdentity")
public class SunatIdentityController {

    private final SunatIdentitySearchService sunatIdentitySearchService;

    public SunatIdentityController(
            SunatIdentitySearchService sunatIdentitySearchService
    ) {
        this.sunatIdentitySearchService = sunatIdentitySearchService;
    }

    @GetMapping("findCompanyByRuc")
    public ResponseEntity<ResponseWsDto> findCompanyByRuc(
            @RequestParam("Ruc") String ruc
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(sunatIdentitySearchService.findCompanyByRuc(ruc)),
                    HttpStatus.OK
            );
        } catch (Exception exception) {
            return new ResponseEntity<>(
                    new ResponseWsDto(exception),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("findPersonByDocument")
    public ResponseEntity<ResponseWsDto> findPersonByDocument(
            @RequestParam("DocumentType") String documentType,
            @RequestParam("DocumentNumber") String documentNumber
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(
                            sunatIdentitySearchService.findPersonByDocument(
                                    documentType,
                                    documentNumber
                            )
                    ),
                    HttpStatus.OK
            );
        } catch (Exception exception) {
            return new ResponseEntity<>(
                    new ResponseWsDto(exception),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
