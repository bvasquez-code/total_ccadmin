package com.local.app.pinpad.adapter.culqi;

import com.local.app.pinpad.enums.PinpadPaymentMethod;

public record CulqiTerminalPaymentRequest(String paymentId, long amountCents, String currency,
                                          PinpadPaymentMethod paymentMethod, String terminalId,
                                          String merchantId, String externalReference, long timeoutSeconds) {
}
