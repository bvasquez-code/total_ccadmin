package com.local.app.pinpad.adapter;

import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.enums.PinpadErrorCode;
import com.local.app.pinpad.enums.PinpadPaymentMethod;
import com.local.app.pinpad.enums.PinpadPaymentStatus;
import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class PinpadSimulatorAdapter implements PinpadAdapter {

    private final PinpadAgentProperties properties;

    public PinpadSimulatorAdapter(PinpadAgentProperties properties) {
        this.properties = properties;
    }

    @Override
    public PinpadAdapterResult processPayment(PinpadPaymentDetailDto payment) {
        sleep();
        PinpadAdapterResult result = new PinpadAdapterResult();
        result.setTerminalId(properties.getTerminalId());
        result.setMerchantId(properties.getMerchantId());
        result.setReferenceNumber(numeric(9));

        PinpadPaymentStatus simulatedStatus = resolveSimulatedStatus(payment);
        if (simulatedStatus == PinpadPaymentStatus.REJECTED) {
            result.setStatus(PinpadPaymentStatus.REJECTED);
            result.setMessage("Pago rechazado");
            result.setErrorCode(PinpadErrorCode.PINPAD_REJECTED.name());
            result.setErrorMessage("Operacion rechazada por simulador");
        } else if (simulatedStatus == PinpadPaymentStatus.TIMEOUT) {
            result.setStatus(PinpadPaymentStatus.TIMEOUT);
            result.setMessage("Timeout esperando respuesta del pinpad");
            result.setErrorCode(PinpadErrorCode.PINPAD_TIMEOUT.name());
            result.setErrorMessage("Timeout simulado");
        } else if (simulatedStatus == PinpadPaymentStatus.ERROR) {
            result.setStatus(PinpadPaymentStatus.ERROR);
            result.setMessage("Error procesando pago");
            result.setErrorCode(PinpadErrorCode.PINPAD_ERROR.name());
            result.setErrorMessage("Error simulado del pinpad");
        } else if (simulatedStatus == PinpadPaymentStatus.CANCELLED) {
            result.setStatus(PinpadPaymentStatus.CANCELLED);
            result.setMessage("Operacion cancelada");
            result.setErrorCode(PinpadErrorCode.PINPAD_CANCELLED.name());
        } else {
            result.setStatus(PinpadPaymentStatus.APPROVED);
            result.setMessage("Pago aprobado");
        }

        if (result.getStatus() == PinpadPaymentStatus.APPROVED) {
            result.setTransactionId("TXN" + numeric(9));
            result.setAuthorizationCode(numeric(6));
            if (payment.getPaymentMethod() == PinpadPaymentMethod.CARD) {
                result.setCardBrand("VISA");
                result.setCardType("DEBIT");
                result.setLastFour("1234");
            } else {
                result.setWalletName(payment.getPaymentMethod().name());
                result.setPaymentMethodDescription(payment.getPaymentMethod().name() + " SIMULADO");
            }
        }
        result.setVoucher(buildVoucher(payment, result));
        return result;
    }

    @Override
    public PinpadAdapterResult cancelPayment(String paymentId) {
        PinpadAdapterResult result = new PinpadAdapterResult();
        result.setStatus(PinpadPaymentStatus.CANCELLED);
        result.setMessage("Operacion cancelada");
        result.setErrorCode(PinpadErrorCode.PINPAD_CANCELLED.name());
        result.setTerminalId(properties.getTerminalId());
        result.setMerchantId(properties.getMerchantId());
        return result;
    }

    @Override
    public String getPinpadStatus() {
        return properties.isSimulatorEnabled() ? "CONNECTED" : "DISCONNECTED";
    }

    private void sleep() {
        try {
            long minDelay = Math.max(0, properties.getSimulatorMinDelayMillis());
            long maxDelay = Math.max(minDelay, properties.getSimulatorMaxDelayMillis());
            long delay = ThreadLocalRandom.current().nextLong(minDelay, maxDelay + 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private PinpadPaymentStatus resolveSimulatedStatus(PinpadPaymentDetailDto payment) {
        int suffix = (int) (payment.getAmountCents() % 100);
        if (suffix == 1) {
            return PinpadPaymentStatus.REJECTED;
        }
        if (suffix == 2) {
            return PinpadPaymentStatus.TIMEOUT;
        }
        if (suffix == 3) {
            return PinpadPaymentStatus.ERROR;
        }
        if (suffix == 4) {
            return PinpadPaymentStatus.CANCELLED;
        }

        int chance = ThreadLocalRandom.current().nextInt(100);
        if (chance < 90) {
            return PinpadPaymentStatus.APPROVED;
        }
        if (chance < 94) {
            return PinpadPaymentStatus.REJECTED;
        }
        if (chance < 97) {
            return PinpadPaymentStatus.TIMEOUT;
        }
        if (chance < 99) {
            return PinpadPaymentStatus.ERROR;
        }
        return PinpadPaymentStatus.CANCELLED;
    }

    private String numeric(int length) {
        String digits = UUID.randomUUID().toString().replaceAll("\\D", "");
        while (digits.length() < length) {
            digits += UUID.randomUUID().toString().replaceAll("\\D", "");
        }
        return digits.substring(0, length);
    }

    private String buildVoucher(PinpadPaymentDetailDto payment, PinpadAdapterResult result) {
        BigDecimal amount = BigDecimal.valueOf(payment.getAmountCents())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return "VOUCHER SIMULADO\n"
                + "COMERCIO DEMO\n"
                + "PAGO: " + payment.getPaymentId() + "\n"
                + "ESTADO: " + result.getStatus() + "\n"
                + "MONTO S/ " + amount.toPlainString() + "\n"
                + "AUT: " + (result.getAuthorizationCode() == null ? "" : result.getAuthorizationCode()) + "\n"
                + "MEDIO: " + payment.getPaymentMethod().name().toUpperCase(Locale.ROOT);
    }
}
