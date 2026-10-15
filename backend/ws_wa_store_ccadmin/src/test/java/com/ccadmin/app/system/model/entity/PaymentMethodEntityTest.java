package com.ccadmin.app.system.model.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentMethodEntityTest {

    @Test
    void processingModeIsIndependentOfTypeAndLimitsAllowedChannels() {
        var method = new PaymentMethodEntity(); method.PaymentMethodCod = "WD001"; method.PaymentMethodType = "1006";
        assertEquals("D", method.validate().PaymentProcessingMode);
        org.junit.jupiter.api.Assertions.assertTrue(method.allowsDirectPayment());
        org.junit.jupiter.api.Assertions.assertFalse(method.allowsIntermediaryPayment());
        method.PaymentProcessingMode = " i "; method.validate();
        org.junit.jupiter.api.Assertions.assertFalse(method.allowsDirectPayment());
        org.junit.jupiter.api.Assertions.assertTrue(method.allowsIntermediaryPayment());
        method.PaymentProcessingMode = "B";
        org.junit.jupiter.api.Assertions.assertTrue(method.allowsDirectPayment());
        org.junit.jupiter.api.Assertions.assertTrue(method.allowsIntermediaryPayment());
        method.PaymentProcessingMode = "INVALID";
        assertThrows(IllegalArgumentException.class, method::validate);
    }

    @Test
    void appliesDefaultsAndNormalizesChannelIndicators() {
        PaymentMethodEntity paymentMethod = new PaymentMethodEntity();
        paymentMethod.PaymentMethodCod = "PAY001";
        paymentMethod.IsInternalSaleEnabled = " s ";
        paymentMethod.IsWebSaleEnabled = "n";
        paymentMethod.IsPaymentProofRequired = "";

        paymentMethod.validate();

        assertEquals("S", paymentMethod.IsInternalSaleEnabled);
        assertEquals("N", paymentMethod.IsWebSaleEnabled);
        assertEquals("N", paymentMethod.IsPaymentProofRequired);
    }

    @Test
    void rejectsPaymentMethodHiddenFromBothSalesChannels() {
        PaymentMethodEntity paymentMethod = new PaymentMethodEntity();
        paymentMethod.PaymentMethodCod = "PAY001";
        paymentMethod.IsInternalSaleEnabled = "N";
        paymentMethod.IsWebSaleEnabled = "N";

        assertThrows(IllegalArgumentException.class, paymentMethod::validate);
    }

    @Test
    void rejectsPaymentProofWhenWebSaleIsDisabled() {
        PaymentMethodEntity paymentMethod = new PaymentMethodEntity();
        paymentMethod.PaymentMethodCod = "PAY001";
        paymentMethod.IsInternalSaleEnabled = "S";
        paymentMethod.IsWebSaleEnabled = "N";
        paymentMethod.IsPaymentProofRequired = "S";

        assertThrows(IllegalArgumentException.class, paymentMethod::validate);
    }
}
