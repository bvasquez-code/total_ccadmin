package com.ccadmin.app.delivery.controller;

import com.ccadmin.app.delivery.model.dto.ClientLoginDto;
import com.ccadmin.app.delivery.model.dto.ClientRegisterDto;
import com.ccadmin.app.delivery.service.ClientAccountDeliveryCreateService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/delivery/clientAccount")
public class ClientAccountDeliveryController {

    private final ClientAccountDeliveryCreateService clientAccountDeliveryCreateService;

    public ClientAccountDeliveryController(
            ClientAccountDeliveryCreateService clientAccountDeliveryCreateService
    ) {
        this.clientAccountDeliveryCreateService = clientAccountDeliveryCreateService;
    }

    @PostMapping("login")
    public ResponseEntity<ResponseWsDto> login(@RequestBody ClientLoginDto request) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(clientAccountDeliveryCreateService.login(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("register")
    public ResponseEntity<ResponseWsDto> register(@RequestBody ClientRegisterDto request) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(clientAccountDeliveryCreateService.register(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
