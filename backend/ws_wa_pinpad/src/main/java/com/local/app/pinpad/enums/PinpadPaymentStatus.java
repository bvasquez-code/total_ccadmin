package com.local.app.pinpad.enums;

public enum PinpadPaymentStatus {
    CREATED,
    PROCESSING,
    APPROVED,
    REJECTED,
    CANCELLED,
    TIMEOUT,
    ERROR,
    UNKNOWN,
    READ;

    public boolean isFinalBeforeRead() {
        return this == APPROVED || this == REJECTED || this == CANCELLED
                || this == TIMEOUT || this == ERROR || this == UNKNOWN;
    }
}
