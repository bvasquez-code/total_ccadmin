package com.local.app.pinpad.adapter;

import com.local.app.pinpad.adapter.niubiz.NiubizTerminalClient;
import com.local.app.pinpad.adapter.niubiz.NiubizTerminalPaymentRequest;
import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.enums.PinpadPaymentMethod;
import com.local.app.pinpad.enums.PinpadPaymentStatus;
import com.local.app.pinpad.exception.PinpadPaymentException;
import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;
import com.local.app.pinpad.model.dto.PinpadPaymentRegisterDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PinpadNiubizAdapterTest {
    private final PinpadAgentProperties pinpadAgentProperties = new PinpadAgentProperties();
    private final NiubizTerminalClient niubizTerminalClient = mock(NiubizTerminalClient.class);
    private final PinpadNiubizAdapter adapter = new PinpadNiubizAdapter(pinpadAgentProperties, niubizTerminalClient);

    @Test
    void delegatesSaleWithStableReferenceExactCentsAndTerminalConfiguration() {
        PinpadPaymentDetailDto payment = new PinpadPaymentDetailDto();
        payment.setPaymentId("niubiz-1");
        payment.setAmountCents(12345L);
        payment.setCurrency("PEN");
        payment.setPaymentMethod(PinpadPaymentMethod.CARD);
        payment.setExternalReference("sale-123");
        when(niubizTerminalClient.processPayment(any())).thenReturn(approved());
        assertThat(adapter.processPayment(payment).getStatus()).isEqualTo(PinpadPaymentStatus.APPROVED);
        ArgumentCaptor<NiubizTerminalPaymentRequest> request = ArgumentCaptor.forClass(NiubizTerminalPaymentRequest.class);
        verify(niubizTerminalClient, times(1)).processPayment(request.capture());
        assertThat(request.getValue()).isEqualTo(new NiubizTerminalPaymentRequest("niubiz-1", 12345L, "PEN",
                PinpadPaymentMethod.CARD, pinpadAgentProperties.getTerminalId(), pinpadAgentProperties.getMerchantId(),
                "sale-123", pinpadAgentProperties.getTimeoutSeconds()));
    }

    @Test
    void transportFailureRemainsUnknownWithoutRetryOrSdkMessage() {
        PinpadPaymentDetailDto payment = new PinpadPaymentDetailDto();
        payment.setAmountCents(100L);
        when(niubizTerminalClient.processPayment(any())).thenThrow(new IllegalStateException("sensitive-sdk-data"));
        PinpadAdapterResult response = adapter.processPayment(payment);
        assertThat(response.getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
        assertThat(response.getMessage()).contains("Niubiz").doesNotContain("sensitive-sdk-data");
        assertThat(response.getErrorMessage()).isNull();
        verify(niubizTerminalClient, times(1)).processPayment(any());
    }

    @Test
    void cancellationPreservesApprovalAndFailedCancellationIsUncertain() {
        when(niubizTerminalClient.cancelPayment("niubiz-1")).thenReturn(approved())
                .thenThrow(new IllegalStateException("disconnected"));
        assertThat(adapter.cancelPayment("niubiz-1").getStatus()).isEqualTo(PinpadPaymentStatus.APPROVED);
        assertThat(adapter.cancelPayment("niubiz-1").getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
    }

    @Test
    void queryUsesExistingReferenceAndNeverCreatesSale() {
        when(niubizTerminalClient.queryPayment("niubiz-1")).thenReturn(Optional.of(approved()));
        assertThat(adapter.queryPayment("niubiz-1").orElseThrow().getTransactionId()).isEqualTo("niubiz-transaction");
        verify(niubizTerminalClient).queryPayment("niubiz-1");
        verify(niubizTerminalClient, never()).processPayment(any());
    }

    @Test
    void queryFailureDoesNotInventRejectionAndHealthReportsDisconnected() {
        when(niubizTerminalClient.queryPayment("niubiz-1")).thenThrow(new IllegalStateException("offline"));
        when(niubizTerminalClient.isConnected()).thenThrow(new IllegalStateException("offline"));
        assertThat(adapter.queryPayment("niubiz-1")).isEmpty();
        assertThat(adapter.getPinpadStatus()).isEqualTo("DISCONNECTED");
    }

    @Test
    void unsupportedWalletIsRejectedBeforeUsingSdk() {
        PinpadPaymentRegisterDto request = new PinpadPaymentRegisterDto();
        request.setPaymentMethod(PinpadPaymentMethod.YAPE);
        assertThatThrownBy(() -> adapter.validatePayment(request)).isInstanceOf(PinpadPaymentException.class);
        verifyNoInteractions(niubizTerminalClient);
    }

    @Test
    void sharedPolicyRejectsMalformedApprovalAndWrongTerminal() {
        PinpadAdapterResult missingTransaction = new PinpadAdapterResult();
        missingTransaction.setStatus(PinpadPaymentStatus.APPROVED);
        PinpadAdapterResult wrongTerminal = approved();
        wrongTerminal.setTerminalId("OTHER-TERMINAL");
        when(niubizTerminalClient.cancelPayment("niubiz-1")).thenReturn(missingTransaction, wrongTerminal);
        assertThat(adapter.cancelPayment("niubiz-1").getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
        assertThat(adapter.cancelPayment("niubiz-1").getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
    }

    private PinpadAdapterResult approved() {
        PinpadAdapterResult result = new PinpadAdapterResult();
        result.setStatus(PinpadPaymentStatus.APPROVED);
        result.setTransactionId("niubiz-transaction");
        return result;
    }
}
