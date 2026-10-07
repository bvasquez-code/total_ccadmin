package com.local.app.pinpad.adapter;

import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;
import com.local.app.pinpad.model.dto.PinpadPaymentRegisterDto;
import java.util.Optional;

public interface PinpadAdapter {

    PinpadAdapterResult processPayment(PinpadPaymentDetailDto payment);

    PinpadAdapterResult cancelPayment(String paymentId);

    String getPinpadStatus();

    default void validatePayment(PinpadPaymentRegisterDto payment) { }

    default boolean requiresReconciliation() { return false; }

    // Consulta por la misma referencia: nunca debe iniciar otro cobro.
    default Optional<PinpadAdapterResult> queryPayment(String paymentId) { return Optional.empty(); }
}
