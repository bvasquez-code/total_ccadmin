package com.ccadmin.app.delivery.controller;

import com.ccadmin.app.delivery.model.dto.SalePaymentDeliveryRegisterDto;
import com.ccadmin.app.delivery.model.dto.SaleDeliveryAccessRequestDto;
import com.ccadmin.app.delivery.service.SaleDeliverySearchService;
import com.ccadmin.app.delivery.service.SalePaymentDeliveryCreateService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/delivery/sale")
public class SaleDeliveryController {

    private final SalePaymentDeliveryCreateService salePaymentDeliveryCreateService;
    private final SaleDeliverySearchService saleDeliverySearchService;

    public SaleDeliveryController(
            SalePaymentDeliveryCreateService salePaymentDeliveryCreateService,
            SaleDeliverySearchService saleDeliverySearchService
    ) {
        this.salePaymentDeliveryCreateService = salePaymentDeliveryCreateService;
        this.saleDeliverySearchService = saleDeliverySearchService;
    }

    @PostMapping("addPayment")
    public ResponseEntity<ResponseWsDto> addPayment(
            @RequestBody SalePaymentDeliveryRegisterDto request
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(salePaymentDeliveryCreateService.save(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(
            @RequestBody SaleDeliveryAccessRequestDto request
    ) {
        try {
            return new ResponseEntity<>(
                    saleDeliverySearchService.findDataForm(request.OrderToken),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findMyOrders")
    public ResponseEntity<ResponseWsDto> findMyOrders(
            @RequestParam(defaultValue = "1") int Page
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(saleDeliverySearchService.findMyOrders(Page)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
