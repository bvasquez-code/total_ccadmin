package com.ccadmin.app.sale.exception;

public class SaleException extends Exception{

    public SaleException() {
        super();
    }

    public SaleException(String message) {
        super(message);
    }

    public SaleException(String message, Throwable cause) {
        super(message, cause);
    }

    public SaleException(Throwable cause) {
        super(cause);
    }

    protected SaleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
