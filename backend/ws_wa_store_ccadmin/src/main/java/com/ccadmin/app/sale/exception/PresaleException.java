package com.ccadmin.app.sale.exception;

public class PresaleException extends Exception{

    public PresaleException() {
        super();
    }

    public PresaleException(String message) {
        super(message);
    }

    public PresaleException(String message, Throwable cause) {
        super(message, cause);
    }

    public PresaleException(Throwable cause) {
        super(cause);
    }

    protected PresaleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
