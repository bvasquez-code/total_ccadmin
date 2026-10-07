package com.local.app.pinpad.adapter;

import com.local.app.pinpad.adapter.culqi.CulqiTerminalClient;
import com.local.app.pinpad.adapter.culqi.CulqiTerminalPaymentRequest;
import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.enums.PinpadErrorCode;
import com.local.app.pinpad.enums.PinpadPaymentStatus;
import com.local.app.pinpad.exception.PinpadPaymentException;
import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;
import com.local.app.pinpad.model.dto.PinpadPaymentRegisterDto;

import java.util.Optional;

public class PinpadCulqiAdapter implements PinpadAdapter {

    private final PinpadAgentProperties pinpadAgentProperties;
    private final CulqiTerminalClient culqiTerminalClient;

    public PinpadCulqiAdapter(PinpadAgentProperties pinpadAgentProperties, CulqiTerminalClient culqiTerminalClient) {
        this.pinpadAgentProperties = pinpadAgentProperties;
        this.culqiTerminalClient = culqiTerminalClient;
    }

    @Override
    public void validatePayment(PinpadPaymentRegisterDto payment) {
        if (!pinpadAgentProperties.getCulqiSupportedPaymentMethods().contains(payment.getPaymentMethod())) {
            throw new PinpadPaymentException(PinpadErrorCode.INVALID_REQUEST,
                    "Medio de pago no habilitado para el terminal Culqi");
        }
    }

    @Override
    public PinpadAdapterResult processPayment(PinpadPaymentDetailDto payment) {
        try {
            return normalize(culqiTerminalClient.processPayment(new CulqiTerminalPaymentRequest(
                    payment.getPaymentId(), payment.getAmountCents(), payment.getCurrency(), payment.getPaymentMethod(),
                    pinpadAgentProperties.getTerminalId(), pinpadAgentProperties.getMerchantId(),
                    payment.getExternalReference(), pinpadAgentProperties.getTimeoutSeconds())));
        } catch (RuntimeException exception) {
            // Un fallo de transporte no demuestra que el banco no haya cobrado.
            return unknownResult();
        }
    }

    @Override
    public PinpadAdapterResult cancelPayment(String paymentId) {
        try {
            return normalize(culqiTerminalClient.cancelPayment(paymentId));
        } catch (RuntimeException exception) {
            return unknownResult();
        }
    }

    @Override
    public Optional<PinpadAdapterResult> queryPayment(String paymentId) {
        try {
            return culqiTerminalClient.queryPayment(paymentId).map(this::normalize);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean requiresReconciliation() { return true; }

    @Override
    public String getPinpadStatus() {
        try {
            return culqiTerminalClient.isConnected() ? "CONNECTED" : "DISCONNECTED";
        } catch (RuntimeException exception) {
            return "DISCONNECTED";
        }
    }

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
        result.setMessage("Resultado Culqi pendiente de conciliacion; consultar el mismo paymentId");
        result.setTerminalId(pinpadAgentProperties.getTerminalId());
        result.setMerchantId(pinpadAgentProperties.getMerchantId());
        return result;
    }
}
