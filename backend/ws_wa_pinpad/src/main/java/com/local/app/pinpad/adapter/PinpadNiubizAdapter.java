package com.local.app.pinpad.adapter;

import com.local.app.pinpad.adapter.niubiz.NiubizTerminalClient;
import com.local.app.pinpad.adapter.niubiz.NiubizTerminalPaymentRequest;
import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;

import java.util.Optional;

public class PinpadNiubizAdapter extends PinpadTerminalAdapter {

    private final NiubizTerminalClient niubizTerminalClient;

    public PinpadNiubizAdapter(PinpadAgentProperties pinpadAgentProperties, NiubizTerminalClient niubizTerminalClient) {
        super(pinpadAgentProperties, "Niubiz", pinpadAgentProperties.getNiubizSupportedPaymentMethods());
        this.niubizTerminalClient = niubizTerminalClient;
    }

    @Override
    protected PinpadAdapterResult processTerminalPayment(PinpadPaymentDetailDto payment) {
        return niubizTerminalClient.processPayment(new NiubizTerminalPaymentRequest(
                payment.getPaymentId(), payment.getAmountCents(), payment.getCurrency(), payment.getPaymentMethod(),
                pinpadAgentProperties.getTerminalId(), pinpadAgentProperties.getMerchantId(),
                payment.getExternalReference(), pinpadAgentProperties.getTimeoutSeconds()));
    }

    @Override
    protected PinpadAdapterResult cancelTerminalPayment(String paymentId) {
        return niubizTerminalClient.cancelPayment(paymentId);
    }

    @Override
    protected Optional<PinpadAdapterResult> queryTerminalPayment(String paymentId) {
        return niubizTerminalClient.queryPayment(paymentId);
    }

    @Override
    protected boolean isTerminalConnected() {
        return niubizTerminalClient.isConnected();
    }
}
