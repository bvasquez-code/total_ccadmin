package com.ccadmin.app.delivery.controller;

import com.ccadmin.app.delivery.service.BillingIdentityDeliverySearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/delivery/billingIdentity")
public class BillingIdentityDeliveryController {

    private final BillingIdentityDeliverySearchService billingIdentityDeliverySearchService;

    public BillingIdentityDeliveryController(
            BillingIdentityDeliverySearchService billingIdentityDeliverySearchService
    ) {
        this.billingIdentityDeliverySearchService = billingIdentityDeliverySearchService;
    }

    @GetMapping("findCompanyByRuc")
    public ResponseEntity<ResponseWsDto> findCompanyByRuc(@RequestParam String Ruc) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(billingIdentityDeliverySearchService.findCompanyByRuc(Ruc)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
