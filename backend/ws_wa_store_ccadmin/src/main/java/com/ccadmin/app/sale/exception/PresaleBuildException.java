package com.ccadmin.app.sale.exception;

public class PresaleBuildException extends RuntimeException{

    public PresaleBuildException() {
        super();
    }

    public PresaleBuildException(String message) {
        super(message);
    }

    public PresaleBuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public PresaleBuildException(Throwable cause) {
        super(cause);
    }

    protected PresaleBuildException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
