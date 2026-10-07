package com.local.app.pinpad.enums;

public enum PinpadProvider {
    DEMO,
    CULQI,
    NIUBIZ;

    public boolean requiresReconciliation() {
        return this != DEMO;
    }
}
