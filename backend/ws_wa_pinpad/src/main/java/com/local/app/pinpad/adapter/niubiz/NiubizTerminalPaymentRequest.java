package com.local.app.pinpad.adapter.niubiz;

import com.local.app.pinpad.enums.PinpadPaymentMethod;

/** Solicitud interna; la implementacion del cliente la traduce al contrato presencial oficial. */
public record NiubizTerminalPaymentRequest(String paymentId, long amountCents, String currency,
                                           PinpadPaymentMethod paymentMethod, String terminalId,
                                           String merchantId, String externalReference, long timeoutSeconds) {
}
