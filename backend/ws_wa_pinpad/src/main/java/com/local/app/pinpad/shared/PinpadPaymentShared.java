package com.local.app.pinpad.shared;

import com.local.app.pinpad.model.dto.PinpadPaymentAckDto;
import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;
import com.local.app.pinpad.model.dto.PinpadPaymentHealthDto;
import com.local.app.pinpad.model.dto.PinpadPaymentRegisterDto;
import com.local.app.pinpad.model.dto.PinpadPaymentSearchDto;
import com.local.app.pinpad.model.dto.PinpadPaymentStatusDto;
import com.local.app.pinpad.model.dto.PinpadVoucherDto;
import com.local.app.pinpad.service.PinpadPaymentCreateService;
import com.local.app.pinpad.service.PinpadPaymentSearchService;
import org.springframework.stereotype.Component;

@Component
public class PinpadPaymentShared {

    private final PinpadPaymentCreateService createService;
    private final PinpadPaymentSearchService searchService;

    public PinpadPaymentShared(PinpadPaymentCreateService createService, PinpadPaymentSearchService searchService) {
        this.createService = createService;
        this.searchService = searchService;
    }

    public PinpadPaymentDetailDto registerPayment(PinpadPaymentRegisterDto request) {
        return createService.registerPayment(request);
    }

    public PinpadPaymentDetailDto ackPayment(String paymentId, PinpadPaymentAckDto ackDto) {
        return createService.ackPayment(paymentId, ackDto);
    }

    public PinpadPaymentDetailDto cancelPayment(String paymentId) {
        return createService.cancelPayment(paymentId);
    }

    public PinpadPaymentStatusDto status(String paymentId) {
        return searchService.status(paymentId);
    }

    public PinpadPaymentSearchDto search(String paymentId) {
        return searchService.search(paymentId);
    }

    public PinpadVoucherDto voucher(String paymentId) {
        return searchService.voucher(paymentId);
    }

    public PinpadPaymentHealthDto health() {
        return searchService.health();
    }
}
