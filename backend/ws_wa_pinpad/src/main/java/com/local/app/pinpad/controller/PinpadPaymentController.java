package com.local.app.pinpad.controller;

import com.local.app.pinpad.model.dto.PinpadPaymentAckDto;
import com.local.app.pinpad.model.dto.PinpadPaymentCancelDto;
import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;
import com.local.app.pinpad.model.dto.PinpadPaymentRegisterDto;
import com.local.app.pinpad.model.dto.PinpadPaymentSearchDto;
import com.local.app.pinpad.model.dto.PinpadPaymentStatusDto;
import com.local.app.pinpad.model.dto.PinpadVoucherDto;
import com.local.app.pinpad.model.dto.ResponseWsDto;
import com.local.app.pinpad.shared.PinpadPaymentShared;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PinpadPaymentController {

    private final PinpadPaymentShared pinpadPaymentShared;

    public PinpadPaymentController(PinpadPaymentShared pinpadPaymentShared) {
        this.pinpadPaymentShared = pinpadPaymentShared;
    }

    @PostMapping("/payment-pinpad")
    public ResponseWsDto<PinpadPaymentStatusDto> registerPayment(@Valid @RequestBody PinpadPaymentRegisterDto request) {
        PinpadPaymentDetailDto detail = pinpadPaymentShared.registerPayment(request);
        return ResponseWsDto.ok(PinpadPaymentStatusDto.fromDetail(detail), detail.getMessage());
    }

    @GetMapping("/payment-pinpad/{paymentId}/status")
    public ResponseWsDto<PinpadPaymentStatusDto> status(@PathVariable String paymentId) {
        PinpadPaymentStatusDto status = pinpadPaymentShared.status(paymentId);
        return ResponseWsDto.ok(status, status.getMessage());
    }

    @PostMapping("/payment-pinpad/{paymentId}/ack")
    public ResponseWsDto<PinpadPaymentStatusDto> ack(@PathVariable String paymentId,
                                                     @RequestBody(required = false) PinpadPaymentAckDto ackDto) {
        PinpadPaymentDetailDto detail = pinpadPaymentShared.ackPayment(paymentId, ackDto);
        return ResponseWsDto.ok(PinpadPaymentStatusDto.fromDetail(detail), detail.getMessage());
    }

    @PostMapping("/payment-pinpad/{paymentId}/cancel")
    public ResponseWsDto<PinpadPaymentCancelDto> cancel(@PathVariable String paymentId) {
        PinpadPaymentDetailDto detail = pinpadPaymentShared.cancelPayment(paymentId);
        PinpadPaymentCancelDto response = new PinpadPaymentCancelDto(
                detail.getPaymentId(), detail.getStatus().name(), detail.getMessage());
        return ResponseWsDto.ok(response, detail.getMessage());
    }

    @GetMapping("/payment-pinpad/{paymentId}/search")
    public ResponseWsDto<PinpadPaymentSearchDto> search(@PathVariable String paymentId) {
        PinpadPaymentSearchDto response = pinpadPaymentShared.search(paymentId);
        return ResponseWsDto.ok(response, response.getMessage());
    }

    @GetMapping("/payment-pinpad/{paymentId}/voucher")
    public ResponseWsDto<PinpadVoucherDto> voucher(@PathVariable String paymentId) {
        PinpadVoucherDto voucher = pinpadPaymentShared.voucher(paymentId);
        return ResponseWsDto.ok(voucher, voucher.getMessage());
    }
}
