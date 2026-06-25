package com.local.app.pinpad.adapter;

import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;

public interface PinpadAdapter {

    PinpadAdapterResult processPayment(PinpadPaymentDetailDto payment);

    PinpadAdapterResult cancelPayment(String paymentId);

    String getPinpadStatus();
}
