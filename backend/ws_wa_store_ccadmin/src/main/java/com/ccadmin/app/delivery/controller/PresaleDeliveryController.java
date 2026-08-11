package com.ccadmin.app.delivery.controller;

import com.ccadmin.app.delivery.model.dto.CheckoutRegisterDto;
import com.ccadmin.app.delivery.service.PresaleDeliveryCreateService;
import com.ccadmin.app.sale.model.dto.PresaleRegisterDto;
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
@RequestMapping("api/v1/delivery/presale")
public class PresaleDeliveryController {

    private final PresaleDeliveryCreateService presaleDeliveryCreateService;

    public PresaleDeliveryController(PresaleDeliveryCreateService presaleDeliveryCreateService) {
        this.presaleDeliveryCreateService = presaleDeliveryCreateService;
    }

    @GetMapping("createCode")
    public ResponseEntity<ResponseWsDto> createCode(@RequestParam String StoreCod) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto().okResponse(
                            presaleDeliveryCreateService.createCode(StoreCod)
                    ),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody CheckoutRegisterDto request) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(presaleDeliveryCreateService.save(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("confirm")
    public ResponseEntity<ResponseWsDto> confirm(@RequestBody PresaleRegisterDto request) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(presaleDeliveryCreateService.confirm(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
