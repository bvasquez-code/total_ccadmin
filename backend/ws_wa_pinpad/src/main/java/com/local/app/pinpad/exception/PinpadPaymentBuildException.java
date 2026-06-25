package com.local.app.pinpad.exception;

import com.local.app.pinpad.enums.PinpadErrorCode;

public class PinpadPaymentBuildException extends PinpadPaymentException {

    public PinpadPaymentBuildException(String message) {
        super(PinpadErrorCode.INVALID_REQUEST, message);
    }
}
