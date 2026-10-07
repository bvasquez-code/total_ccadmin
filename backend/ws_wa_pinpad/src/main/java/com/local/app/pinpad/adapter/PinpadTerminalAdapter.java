package com.local.app.pinpad.adapter;

import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.enums.PinpadErrorCode;
import com.local.app.pinpad.enums.PinpadPaymentMethod;
import com.local.app.pinpad.enums.PinpadPaymentStatus;
import com.local.app.pinpad.exception.PinpadPaymentException;
import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;
import com.local.app.pinpad.model.dto.PinpadPaymentRegisterDto;

import java.util.List;
import java.util.Optional;

/** Politica comun de los terminales fisicos; cada proveedor traduce su contrato en las operaciones protegidas. */
public abstract class PinpadTerminalAdapter implements PinpadAdapter {

    protected final PinpadAgentProperties pinpadAgentProperties;
    private final String providerName;
    private final List<PinpadPaymentMethod> supportedPaymentMethods;

    protected PinpadTerminalAdapter(PinpadAgentProperties pinpadAgentProperties, String providerName,
                                     List<PinpadPaymentMethod> supportedPaymentMethods) {
        this.pinpadAgentProperties = pinpadAgentProperties;
        this.providerName = providerName;
        this.supportedPaymentMethods = List.copyOf(supportedPaymentMethods);
    }

    @Override
    public final void validatePayment(PinpadPaymentRegisterDto payment) {
        if (!supportedPaymentMethods.contains(payment.getPaymentMethod())) {
            throw new PinpadPaymentException(PinpadErrorCode.INVALID_REQUEST,
                    "Medio de pago no habilitado para el terminal " + providerName);
        }
    }

    @Override
    public final PinpadAdapterResult processPayment(PinpadPaymentDetailDto payment) {
        try {
            return normalize(processTerminalPayment(payment));
        } catch (RuntimeException exception) {
            // Un fallo de transporte no demuestra que el banco no haya cobrado.
            return unknownResult();
        }
    }

    @Override
    public final PinpadAdapterResult cancelPayment(String paymentId) {
        try {
            return normalize(cancelTerminalPayment(paymentId));
        } catch (RuntimeException exception) {
            return unknownResult();
        }
    }

    @Override
    public final Optional<PinpadAdapterResult> queryPayment(String paymentId) {
        try {
            return queryTerminalPayment(paymentId).map(this::normalize);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public final boolean requiresReconciliation() { return true; }

    @Override
    public final String getPinpadStatus() {
        try {
            return isTerminalConnected() ? "CONNECTED" : "DISCONNECTED";
        } catch (RuntimeException exception) {
            return "DISCONNECTED";
        }
    }

    protected abstract PinpadAdapterResult processTerminalPayment(PinpadPaymentDetailDto payment);
    protected abstract PinpadAdapterResult cancelTerminalPayment(String paymentId);
    protected abstract Optional<PinpadAdapterResult> queryTerminalPayment(String paymentId);
    protected abstract boolean isTerminalConnected();

    private PinpadAdapterResult normalize(PinpadAdapterResult result) {
        if (result == null || result.getStatus() == null
                || result.getStatus() == PinpadPaymentStatus.CREATED || result.getStatus() == PinpadPaymentStatus.READ
                || result.getStatus() == PinpadPaymentStatus.TIMEOUT || result.getStatus() == PinpadPaymentStatus.ERROR
                || result.getStatus() == PinpadPaymentStatus.UNKNOWN) {
            return unknownResult();
        }
        if (result.getStatus() == PinpadPaymentStatus.APPROVED
                && (result.getTransactionId() == null || result.getTransactionId().isBlank())) {
            return unknownResult();
        }
        if ((result.getTerminalId() != null && !pinpadAgentProperties.getTerminalId().equals(result.getTerminalId()))
                || (result.getMerchantId() != null && !pinpadAgentProperties.getMerchantId().equals(result.getMerchantId()))
                || (result.getLastFour() != null && !result.getLastFour().matches("[0-9]{4}"))) {
            return unknownResult();
        }
        result.setTerminalId(pinpadAgentProperties.getTerminalId());
        result.setMerchantId(pinpadAgentProperties.getMerchantId());
        return result;
    }

    private PinpadAdapterResult unknownResult() {
        PinpadAdapterResult result = new PinpadAdapterResult();
        result.setStatus(PinpadPaymentStatus.UNKNOWN);
        result.setErrorCode(PinpadErrorCode.PINPAD_RESULT_UNCONFIRMED.name());
        result.setMessage("Resultado " + providerName + " pendiente de conciliacion; consultar el mismo paymentId");
        result.setTerminalId(pinpadAgentProperties.getTerminalId());
        result.setMerchantId(pinpadAgentProperties.getMerchantId());
        return result;
    }
}
