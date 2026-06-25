package com.local.app.pinpad.controller;

import com.local.app.pinpad.model.dto.PinpadPaymentHealthDto;
import com.local.app.pinpad.model.dto.ResponseWsDto;
import com.local.app.pinpad.shared.PinpadPaymentShared;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PinpadHealthController {

    private final PinpadPaymentShared pinpadPaymentShared;

    public PinpadHealthController(PinpadPaymentShared pinpadPaymentShared) {
        this.pinpadPaymentShared = pinpadPaymentShared;
    }

    @GetMapping("/health")
    public ResponseWsDto<PinpadPaymentHealthDto> health() {
        return ResponseWsDto.ok(pinpadPaymentShared.health(), "Agente pinpad operativo");
    }
}
