package com.local.app.pinpad.service;

import com.local.app.pinpad.model.dto.PinpadPaymentAckDto;
import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;
import com.local.app.pinpad.model.dto.PinpadPaymentRegisterDto;
import org.springframework.stereotype.Service;

@Service
public class PinpadPaymentCreateService {

    private final PinpadPaymentService paymentService;

    public PinpadPaymentCreateService(PinpadPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public PinpadPaymentDetailDto registerPayment(PinpadPaymentRegisterDto request) {
        return paymentService.registerPayment(request);
    }

    public PinpadPaymentDetailDto ackPayment(String paymentId, PinpadPaymentAckDto ackDto) {
        return paymentService.ackPayment(paymentId, ackDto);
    }

    public PinpadPaymentDetailDto cancelPayment(String paymentId) {
        return paymentService.cancelPayment(paymentId);
    }
}
