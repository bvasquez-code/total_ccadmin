package com.ccadmin.app.sale.exception;

public class SalePaymentException extends Exception{

    public SalePaymentException() {
        super();
    }

    public SalePaymentException(String message) {
        super(message);
    }

    public SalePaymentException(String message, Throwable cause) {
        super(message, cause);
    }

    public SalePaymentException(Throwable cause) {
        super(cause);
    }

    protected SalePaymentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
