package com.ccadmin.app.sale.controller;

import com.ccadmin.app.sale.model.dto.SaleDeliveryStatusChangeDto;
import com.ccadmin.app.sale.service.SaleDeliveryCreateService;
import com.ccadmin.app.sale.service.SaleWebSearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
@RequestMapping("api/v1/saleWeb")
public class SaleWebController {

    private static final Logger log = LogManager.getLogger(SaleWebController.class);

    @Autowired
    private SaleWebSearchService saleWebSearchService;
    @Autowired
    private SaleDeliveryCreateService saleDeliveryCreateService;

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(
            @RequestParam String Query,
            @RequestParam int Page,
            @RequestParam(required = false, defaultValue = "") String DeliveryTypeCod,
            @RequestParam(required = false, defaultValue = "") String DeliveryStatus
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.saleWebSearchService.findAll(
                            Query,
                            Page,
                            DeliveryTypeCod,
                            DeliveryStatus
                    )),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            log.error("Error :{}", ex.getMessage(), ex);
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("changeDeliveryStatus")
    public ResponseEntity<ResponseWsDto> changeDeliveryStatus(
            @RequestBody SaleDeliveryStatusChangeDto request
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.saleDeliveryCreateService.changeStatus(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            log.error("Error :{}", ex.getMessage(), ex);
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
