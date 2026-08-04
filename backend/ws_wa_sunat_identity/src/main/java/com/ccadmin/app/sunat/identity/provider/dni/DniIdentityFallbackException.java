package com.ccadmin.app.sunat.identity.provider.dni;

public class DniIdentityFallbackException extends RuntimeException {

    public DniIdentityFallbackException(String message) {
        super(message);
    }

    public DniIdentityFallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
