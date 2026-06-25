package com.ccadmin.app.pucharse.exception;

public class PucharseException extends Exception{

    public PucharseException() {
    }

    public PucharseException(String message) {
        super(message);
    }

    public PucharseException(String message, Throwable cause) {
        super(message, cause);
    }

    public PucharseException(Throwable cause) {
        super(cause);
    }

    public PucharseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
