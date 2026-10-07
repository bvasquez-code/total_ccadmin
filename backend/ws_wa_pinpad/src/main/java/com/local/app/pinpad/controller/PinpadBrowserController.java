package com.local.app.pinpad.controller;

import com.local.app.pinpad.model.dto.PinpadSignedMessageDto;
import com.local.app.pinpad.model.dto.ResponseWsDto;
import com.local.app.pinpad.service.PinpadBrowserCreateService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import com.local.app.pinpad.service.PinpadAccessCreateService;
import com.local.app.pinpad.model.dto.PinpadLoginRequestDto;
import com.local.app.pinpad.model.dto.PinpadLoginResponseDto;

@RestController
public class PinpadBrowserController {
    private final PinpadBrowserCreateService pinpadBrowserCreateService;
    private final PinpadAccessCreateService pinpadAccessCreateService;
    public PinpadBrowserController(PinpadBrowserCreateService pinpadBrowserCreateService, PinpadAccessCreateService pinpadAccessCreateService) {
        this.pinpadBrowserCreateService = pinpadBrowserCreateService;
        this.pinpadAccessCreateService = pinpadAccessCreateService;
    }
    @PostMapping("/pinpad/login")
    public ResponseWsDto<PinpadLoginResponseDto> login(@RequestBody PinpadLoginRequestDto request) {
        return ResponseWsDto.ok(pinpadAccessCreateService.login(request), "Acceso pinpad autorizado");
    }

    @PostMapping("/pinpad/payment/register")
    public ResponseWsDto<PinpadSignedMessageDto> register(@RequestHeader("Authorization") String authorization) {
        return ResponseWsDto.ok(pinpadBrowserCreateService.execute(authorization, "REGISTER"), "Cobro pinpad procesado");
    }

    @PostMapping("/pinpad/payment/status")
    public ResponseWsDto<PinpadSignedMessageDto> status(@RequestHeader("Authorization") String authorization) {
        return ResponseWsDto.ok(pinpadBrowserCreateService.execute(authorization, "STATUS"), "Estado pinpad consultado");
    }

    @PostMapping("/pinpad/payment/ack")
    public ResponseWsDto<PinpadSignedMessageDto> acknowledge(@RequestHeader("Authorization") String authorization) {
        return ResponseWsDto.ok(pinpadBrowserCreateService.execute(authorization, "ACK"), "Guardado central confirmado");
    }
}
