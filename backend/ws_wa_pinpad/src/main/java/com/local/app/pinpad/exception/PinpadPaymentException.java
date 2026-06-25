package com.local.app.pinpad.exception;

import com.local.app.pinpad.enums.PinpadErrorCode;
import org.springframework.http.HttpStatus;

public class PinpadPaymentException extends RuntimeException {

    private final PinpadErrorCode errorCode;
    private final HttpStatus httpStatus;

    public PinpadPaymentException(PinpadErrorCode errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public PinpadPaymentException(PinpadErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public PinpadPaymentException(PinpadErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public PinpadErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
