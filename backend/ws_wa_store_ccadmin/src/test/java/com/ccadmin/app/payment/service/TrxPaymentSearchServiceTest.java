package com.ccadmin.app.payment.service;

import com.ccadmin.app.system.model.dto.IndicatorDto;
import com.ccadmin.app.system.shared.CurrencyShared;
import com.ccadmin.app.system.shared.PaymentMethodShared;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrxPaymentSearchServiceTest {
    @Test void formIncludesAuthoritativeIntegrationIndicatorForManualAndConnectedModes() {
        var configurations = mock(PinpadConfigurationSearchService.class);
        var service = new TrxPaymentSearchService();
        ReflectionTestUtils.setField(service, "pinpadConfigurationSearchService", configurations);
        ReflectionTestUtils.setField(service, "paymentMethodShared", mock(PaymentMethodShared.class));
        ReflectionTestUtils.setField(service, "currencyShared", mock(CurrencyShared.class));
        for (boolean enabled : new boolean[]{false, true}) {
            when(configurations.isIntegrationEnabled()).thenReturn(enabled);
            var field = service.findDataForm().DataAdditional.stream().filter(item -> "IndPinpadIntegrated".equals(item.Name)).findFirst().orElseThrow();
            var indicator = (IndicatorDto) field.Data;
            assertEquals("IND_PINPAD_INTEGRATED", indicator.Indicator);
            assertEquals(enabled ? "S" : "N", indicator.Value);
        }
    }
}
