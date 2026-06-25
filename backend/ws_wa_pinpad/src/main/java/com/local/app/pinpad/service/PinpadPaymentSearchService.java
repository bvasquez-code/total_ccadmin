package com.local.app.pinpad.service;

import com.local.app.pinpad.enums.PinpadErrorCode;
import com.local.app.pinpad.exception.PinpadPaymentException;
import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;
import com.local.app.pinpad.model.dto.PinpadPaymentHealthDto;
import com.local.app.pinpad.model.dto.PinpadPaymentSearchDto;
import com.local.app.pinpad.model.dto.PinpadPaymentStatusDto;
import com.local.app.pinpad.model.dto.PinpadVoucherDto;
import org.springframework.stereotype.Service;

@Service
public class PinpadPaymentSearchService {

    private final PinpadPaymentService paymentService;

    public PinpadPaymentSearchService(PinpadPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public PinpadPaymentStatusDto status(String paymentId) {
        return PinpadPaymentStatusDto.fromDetail(paymentService.findPayment(paymentId));
    }

    public PinpadPaymentSearchDto search(String paymentId) {
        return paymentService.searchPayment(paymentId)
                .map(PinpadPaymentSearchDto::found)
                .orElseGet(() -> PinpadPaymentSearchDto.notFound(paymentId));
    }

    public PinpadVoucherDto voucher(String paymentId) {
        PinpadPaymentDetailDto detail = paymentService.findPayment(paymentId);
        if (detail.getVoucher() == null || detail.getVoucher().isBlank()) {
            throw new PinpadPaymentException(PinpadErrorCode.PAYMENT_NOT_FOUND,
                    "No existe voucher local para el pago indicado");
        }
        return new PinpadVoucherDto(paymentId, detail.getVoucher(), "Voucher encontrado");
    }

    public PinpadPaymentHealthDto health() {
        return paymentService.health();
    }
}
