package com.ccadmin.app.system.model.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentMethodEntityTest {

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
