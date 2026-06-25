package com.ccadmin.app.product.exception;

public class ProductBuildException extends RuntimeException{

    public ProductBuildException() {
        super();
    }

    public ProductBuildException(String message) {
        super(message);
    }

    public ProductBuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProductBuildException(Throwable cause) {
        super(cause);
    }

    protected ProductBuildException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
