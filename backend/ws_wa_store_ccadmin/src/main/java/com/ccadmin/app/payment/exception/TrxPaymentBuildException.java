package com.ccadmin.app.payment.exception;

public class TrxPaymentBuildException extends RuntimeException{

    public TrxPaymentBuildException() {
        super();
    }

    public TrxPaymentBuildException(String message) {
        super(message);
    }

    public TrxPaymentBuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public TrxPaymentBuildException(Throwable cause) {
        super(cause);
    }

    protected TrxPaymentBuildException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
