package com.local.app.pinpad.adapter;

import com.local.app.pinpad.adapter.culqi.CulqiTerminalClient;
import com.local.app.pinpad.adapter.culqi.CulqiTerminalPaymentRequest;
import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;

import java.util.Optional;

public class PinpadCulqiAdapter extends PinpadTerminalAdapter {

    private final CulqiTerminalClient culqiTerminalClient;

    public PinpadCulqiAdapter(PinpadAgentProperties pinpadAgentProperties, CulqiTerminalClient culqiTerminalClient) {
        super(pinpadAgentProperties, "Culqi", pinpadAgentProperties.getCulqiSupportedPaymentMethods());
        this.culqiTerminalClient = culqiTerminalClient;
    }

    @Override
    protected PinpadAdapterResult processTerminalPayment(PinpadPaymentDetailDto payment) {
        return culqiTerminalClient.processPayment(new CulqiTerminalPaymentRequest(
                payment.getPaymentId(), payment.getAmountCents(), payment.getCurrency(), payment.getPaymentMethod(),
                pinpadAgentProperties.getTerminalId(), pinpadAgentProperties.getMerchantId(),
                payment.getExternalReference(), pinpadAgentProperties.getTimeoutSeconds()));
    }

    @Override
    protected PinpadAdapterResult cancelTerminalPayment(String paymentId) {
        return culqiTerminalClient.cancelPayment(paymentId);
    }

    @Override
    protected Optional<PinpadAdapterResult> queryTerminalPayment(String paymentId) {
        return culqiTerminalClient.queryPayment(paymentId);
    }

    @Override
    protected boolean isTerminalConnected() {
        return culqiTerminalClient.isConnected();
    }
}
