package com.ccadmin.app.delivery.model.dto;

import com.ccadmin.app.payment.model.entity.MercadoPagoAttemptEntity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MercadoPagoResultDtoTest {
    @Test
    void exposesProviderReasonAndTestGuidanceWithoutTreatingARejectionAsApproval() {
        var attempt = rejectedAttempt();
        attempt.TestMode = "S";
        var result = MercadoPagoResultDto.from(attempt);
        assertEquals("F", result.State);
        assertEquals("rejected", result.ProviderStatus);
        assertEquals("cc_rejected_other_reason", result.ProviderStatusDetail);
        assertTrue(result.Message.contains("APRO"));
    }

    @Test
    void productionDoesNotSuggestSimulatedPaymentData() {
        var attempt = rejectedAttempt();
        attempt.TestMode = "N";
        var result = MercadoPagoResultDto.from(attempt);
        assertFalse(result.Message.contains("APRO"));
        assertTrue(result.Message.contains("emisor"));
    }

    @Test
    void olderAttemptsWithoutARecordedDetailKeepTheirRejection() {
        var attempt = rejectedAttempt();
        attempt.ProviderStatusDetail = null;
        var result = MercadoPagoResultDto.from(attempt);
        assertEquals("F", result.State);
        assertNull(result.ProviderStatusDetail);
        assertNotNull(result.Message);
    }

    private MercadoPagoAttemptEntity rejectedAttempt() {
        var attempt = new MercadoPagoAttemptEntity();
        attempt.PaymentState = "F";
        attempt.ProviderStatus = "rejected";
        attempt.ProviderStatusDetail = "cc_rejected_other_reason";
        return attempt;
    }
}
