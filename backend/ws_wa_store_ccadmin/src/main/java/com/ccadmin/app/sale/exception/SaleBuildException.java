package com.ccadmin.app.sale.exception;

public class SaleBuildException extends RuntimeException{

    public SaleBuildException() {
        super();
    }

    public SaleBuildException(String message) {
        super(message);
    }

    public SaleBuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public SaleBuildException(Throwable cause) {
        super(cause);
    }

    protected SaleBuildException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
