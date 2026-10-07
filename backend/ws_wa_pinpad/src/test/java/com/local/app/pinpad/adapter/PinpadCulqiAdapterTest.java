package com.local.app.pinpad.adapter;

import com.local.app.pinpad.adapter.culqi.CulqiTerminalClient;
import com.local.app.pinpad.adapter.culqi.CulqiTerminalPaymentRequest;
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

class PinpadCulqiAdapterTest {
    private final PinpadAgentProperties properties = new PinpadAgentProperties();
    private final CulqiTerminalClient culqiTerminalClient = mock(CulqiTerminalClient.class);
    private final PinpadCulqiAdapter adapter = new PinpadCulqiAdapter(properties, culqiTerminalClient);

    @Test
    void translatesCentsAndStableReferenceWithoutRetry() {
        PinpadPaymentDetailDto payment = new PinpadPaymentDetailDto();
        payment.setPaymentId("payment-1");
        payment.setAmountCents(12345L);
        payment.setCurrency("PEN");
        payment.setPaymentMethod(PinpadPaymentMethod.CARD);
        PinpadAdapterResult approval = result(PinpadPaymentStatus.APPROVED);
        approval.setTransactionId("culqi-transaction");
        when(culqiTerminalClient.processPayment(any())).thenReturn(approval);
        assertThat(adapter.processPayment(payment).getTransactionId()).isEqualTo("culqi-transaction");
        ArgumentCaptor<CulqiTerminalPaymentRequest> request = ArgumentCaptor.forClass(CulqiTerminalPaymentRequest.class);
        verify(culqiTerminalClient, times(1)).processPayment(request.capture());
        assertThat(request.getValue().amountCents()).isEqualTo(12345L);
        assertThat(request.getValue().paymentId()).isEqualTo("payment-1");
    }

    @Test
    void transportFailureIsUnknownAndDoesNotExposeSdkMessage() {
        when(culqiTerminalClient.cancelPayment("payment-1")).thenThrow(new IllegalStateException("sensitive-sdk-data"));
        PinpadAdapterResult response = adapter.cancelPayment("payment-1");
        assertThat(response.getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
        assertThat(response.getErrorMessage()).isNull();
        assertThat(response.getMessage()).doesNotContain("sensitive-sdk-data");
        verify(culqiTerminalClient, times(1)).cancelPayment("payment-1");
    }

    @Test
    void cancellationCanDiscoverApproval() {
        PinpadAdapterResult approval = result(PinpadPaymentStatus.APPROVED);
        approval.setTransactionId("culqi-transaction");
        when(culqiTerminalClient.cancelPayment("payment-1")).thenReturn(approval);
        assertThat(adapter.cancelPayment("payment-1").getStatus()).isEqualTo(PinpadPaymentStatus.APPROVED);
    }

    @Test
    void timeoutOrMalformedApprovalRequiresReconciliation() {
        when(culqiTerminalClient.cancelPayment("payment-1")).thenReturn(result(PinpadPaymentStatus.TIMEOUT),
                result(PinpadPaymentStatus.APPROVED), null);
        for (int attempt = 0; attempt < 3; attempt++) {
            assertThat(adapter.cancelPayment("payment-1").getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
        }
    }

    @Test
    void queryNeverCreatesAnotherPayment() {
        when(culqiTerminalClient.queryPayment("payment-1")).thenReturn(Optional.empty());
        assertThat(adapter.queryPayment("payment-1")).isEmpty();
        verify(culqiTerminalClient).queryPayment("payment-1");
        verify(culqiTerminalClient, never()).processPayment(any());
    }

    @Test
    void unsupportedWalletIsRejectedBeforeCallingClient() {
        PinpadPaymentRegisterDto request = new PinpadPaymentRegisterDto();
        request.setPaymentMethod(PinpadPaymentMethod.YAPE);
        assertThatThrownBy(() -> adapter.validatePayment(request)).isInstanceOf(PinpadPaymentException.class);
        verifyNoInteractions(culqiTerminalClient);
    }

    @Test
    void rejectsResponseForAnotherTerminalOrFullCardNumber() {
        PinpadAdapterResult wrongTerminal = result(PinpadPaymentStatus.REJECTED);
        wrongTerminal.setTerminalId("different-terminal");
        PinpadAdapterResult unsafeCard = result(PinpadPaymentStatus.REJECTED);
        unsafeCard.setLastFour("4111111111111111");
        when(culqiTerminalClient.cancelPayment("payment-1")).thenReturn(wrongTerminal, unsafeCard);
        assertThat(adapter.cancelPayment("payment-1").getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
        assertThat(adapter.cancelPayment("payment-1").getLastFour()).isNull();
    }

    private PinpadAdapterResult result(PinpadPaymentStatus status) {
        PinpadAdapterResult result = new PinpadAdapterResult();
        result.setStatus(status);
        return result;
    }
}
