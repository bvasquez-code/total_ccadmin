package com.local.app.pinpad.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.local.app.pinpad.adapter.PinpadAdapterResult;
import com.local.app.pinpad.adapter.PinpadCulqiAdapter;
import com.local.app.pinpad.adapter.PinpadNiubizAdapter;
import com.local.app.pinpad.adapter.PinpadSimulatorAdapter;
import com.local.app.pinpad.adapter.culqi.CulqiTerminalClient;
import com.local.app.pinpad.adapter.niubiz.NiubizTerminalClient;
import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.enums.PinpadPaymentMethod;
import com.local.app.pinpad.enums.PinpadPaymentStatus;
import com.local.app.pinpad.enums.PinpadProvider;
import com.local.app.pinpad.exception.PinpadPaymentException;
import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;
import com.local.app.pinpad.model.dto.PinpadPaymentRegisterDto;
import com.local.app.pinpad.repository.PinpadPaymentFileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PinpadPaymentServiceTest {
    @TempDir Path storage;
    private final PinpadAgentProperties properties = new PinpadAgentProperties();
    private final CulqiTerminalClient culqiTerminalClient = mock(CulqiTerminalClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private PinpadPaymentFileRepository paymentFileRepository;
    private PinpadPaymentService paymentService;

    @BeforeEach
    void initialize() {
        properties.setProvider(PinpadProvider.CULQI);
        properties.setStoragePath(storage.toString());
        properties.setTerminalId("TERM-001");
        properties.setMerchantId("MERCHANT-001");
        paymentFileRepository = new PinpadPaymentFileRepository(properties, objectMapper);
        paymentFileRepository.initializeStorage();
        paymentService = createService();
    }

    @AfterEach
    void stopExecutor() { paymentService.shutdown(); }

    @Test
    void duplicateRegistrationNeverChargesTwiceAndRejectsChangedAmount() {
        when(culqiTerminalClient.processPayment(any())).thenReturn(approved());
        assertThat(paymentService.registerPayment(request("payment-1")).getStatus()).isEqualTo(PinpadPaymentStatus.APPROVED);
        assertThat(paymentService.registerPayment(request("payment-1")).getStatus()).isEqualTo(PinpadPaymentStatus.APPROVED);
        PinpadPaymentRegisterDto changed = request("payment-1");
        changed.setAmountCents(999L);
        assertThatThrownBy(() -> paymentService.registerPayment(changed)).isInstanceOf(PinpadPaymentException.class);
        verify(culqiTerminalClient, times(1)).processPayment(any());
    }

    @Test
    void unknownResultBlocksNewPaymentsAndAckUntilReconciled() {
        when(culqiTerminalClient.processPayment(any())).thenThrow(new IllegalStateException("transport failure"));
        assertThat(paymentService.registerPayment(request("payment-1")).getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
        assertThatThrownBy(() -> paymentService.registerPayment(request("payment-2"))).isInstanceOf(PinpadPaymentException.class);
        assertThatThrownBy(() -> paymentService.ackPayment("payment-1", null)).isInstanceOf(PinpadPaymentException.class);
        when(culqiTerminalClient.queryPayment("payment-1")).thenReturn(Optional.of(approved()));
        assertThat(paymentService.searchPayment("payment-1").orElseThrow().getStatus()).isEqualTo(PinpadPaymentStatus.APPROVED);
        assertThat(Files.exists(storage.resolve("unknown/payment-1.json"))).isFalse();
        assertThat(paymentService.ackPayment("payment-1", null).getStatus()).isEqualTo(PinpadPaymentStatus.READ);
        verify(culqiTerminalClient, times(1)).processPayment(any());
    }

    @Test
    void restartRestoresUnknownAndQueriesWithoutResubmitting() {
        when(culqiTerminalClient.processPayment(any())).thenReturn(result(PinpadPaymentStatus.UNKNOWN));
        paymentService.registerPayment(request("payment-1"));
        paymentService.shutdown();
        paymentService = createService();
        paymentService.restoreActivePayment();
        assertThat(paymentService.health().getActivePaymentId()).isEqualTo("payment-1");
        assertThatThrownBy(() -> paymentService.registerPayment(request("payment-2"))).isInstanceOf(PinpadPaymentException.class);
        when(culqiTerminalClient.queryPayment("payment-1")).thenReturn(Optional.of(approved()));
        assertThat(paymentService.findPayment("payment-1").getStatus()).isEqualTo(PinpadPaymentStatus.APPROVED);
        verify(culqiTerminalClient, times(1)).processPayment(any());
    }

    @Test
    void timeoutAfterRestartRemainsUncertainInsteadOfFreeingTerminal() {
        PinpadPaymentDetailDto interrupted = persistedPayment("interrupted", PinpadPaymentStatus.PROCESSING);
        interrupted.setStartedAt(LocalDateTime.now().minusMinutes(10));
        paymentFileRepository.saveProcessing(interrupted);
        paymentService.restoreActivePayment();
        assertThat(paymentService.findPayment("interrupted").getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
        assertThatThrownBy(() -> paymentService.registerPayment(request("payment-2"))).isInstanceOf(PinpadPaymentException.class);
        verify(culqiTerminalClient, never()).processPayment(any());
        when(culqiTerminalClient.queryPayment("interrupted")).thenReturn(Optional.of(approved()));
        assertThat(paymentService.findPayment("interrupted").getStatus()).isEqualTo(PinpadPaymentStatus.APPROVED);
    }

    @Test
    void cancellationResponseCanBeApprovalAndIsNotForcedToCancelled() {
        paymentFileRepository.saveProcessing(persistedPayment("payment-1", PinpadPaymentStatus.PROCESSING));
        when(culqiTerminalClient.cancelPayment("payment-1")).thenReturn(approved());
        assertThat(paymentService.cancelPayment("payment-1").getStatus()).isEqualTo(PinpadPaymentStatus.APPROVED);
        assertThat(paymentService.findPayment("payment-1").getTransactionId()).isEqualTo("culqi-transaction");
    }

    @Test
    void failedCancellationRemainsUnknownAndBlocksOtherCharges() {
        paymentFileRepository.saveProcessing(persistedPayment("payment-1", PinpadPaymentStatus.PROCESSING));
        when(culqiTerminalClient.cancelPayment("payment-1")).thenThrow(new IllegalStateException("offline"));
        assertThat(paymentService.cancelPayment("payment-1").getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
        assertThatThrownBy(() -> paymentService.registerPayment(request("payment-2"))).isInstanceOf(PinpadPaymentException.class);
    }

    @Test
    void lateProcessingErrorCannotOverwriteConfirmedCancellationOrAck() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        properties.setWaitFinalResultOnRegister(false);
        when(culqiTerminalClient.processPayment(any())).thenAnswer(invocation -> {
            started.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test release timeout");
            }
            throw new IllegalStateException("late failure");
        });
        when(culqiTerminalClient.cancelPayment("payment-1")).thenReturn(result(PinpadPaymentStatus.CANCELLED));
        paymentService.registerPayment(request("payment-1"));
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
        try {
            assertThat(paymentService.cancelPayment("payment-1").getStatus()).isEqualTo(PinpadPaymentStatus.CANCELLED);
            paymentService.ackPayment("payment-1", null);
            // No empezar una nueva llamada al equipo mientras la anterior siga ejecutandose.
            assertThatThrownBy(() -> paymentService.registerPayment(request("payment-2"))).isInstanceOf(PinpadPaymentException.class);
        } finally {
            release.countDown();
        }
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            while (paymentService.health().getActivePaymentId() != null) {
                Thread.sleep(10);
            }
        });
        assertThat(paymentService.findPayment("payment-1").getStatus()).isEqualTo(PinpadPaymentStatus.READ);
    }

    @Test
    void lateApprovalResolvesEarlierLocalTimeout() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        properties.setWaitFinalResultOnRegister(false);
        when(culqiTerminalClient.processPayment(any())).thenAnswer(invocation -> {
            started.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test release timeout");
            }
            return approved();
        });
        paymentService.registerPayment(request("payment-1"));
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
        try {
            PinpadPaymentDetailDto expired = paymentFileRepository.findByPaymentId("payment-1").orElseThrow();
            expired.setStartedAt(LocalDateTime.now().minusMinutes(10));
            paymentFileRepository.saveProcessing(expired);
            assertThat(paymentService.findPayment("payment-1").getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
        } finally {
            release.countDown();
        }
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            while (paymentService.health().getActivePaymentId() != null) {
                Thread.sleep(10);
            }
        });
        assertThat(paymentService.findPayment("payment-1").getStatus()).isEqualTo(PinpadPaymentStatus.APPROVED);
    }

    @Test
    void demoResultsCannotBeReusedAsCulqiResults() {
        PinpadPaymentDetailDto oldDemo = persistedPayment("payment-1", PinpadPaymentStatus.APPROVED);
        oldDemo.setProvider(null); // Archivos beta anteriores al selector.
        paymentFileRepository.saveFinal(oldDemo);
        assertThatThrownBy(() -> paymentService.registerPayment(request("payment-1"))).isInstanceOf(PinpadPaymentException.class);
        verifyNoInteractions(culqiTerminalClient);
    }

    @Test
    void invalidAmountOrUnsupportedMethodCreatesNoLocalPayment() {
        PinpadPaymentRegisterDto invalidAmount = request("amount");
        invalidAmount.setAmount(new BigDecimal("999.99"));
        assertThatThrownBy(() -> paymentService.registerPayment(invalidAmount)).isInstanceOf(PinpadPaymentException.class);
        PinpadPaymentRegisterDto wallet = request("wallet");
        wallet.setPaymentMethod(PinpadPaymentMethod.YAPE);
        assertThatThrownBy(() -> paymentService.registerPayment(wallet)).isInstanceOf(PinpadPaymentException.class);
        assertThat(paymentFileRepository.findByPaymentId("amount")).isEmpty();
        assertThat(paymentFileRepository.findByPaymentId("wallet")).isEmpty();
        verifyNoInteractions(culqiTerminalClient);
    }

    @Test
    void pendingPaymentCannotBeQueriedOrCancelledOnAnotherTerminal() {
        PinpadPaymentDetailDto oldTerminal = persistedPayment("payment-1", PinpadPaymentStatus.UNKNOWN);
        oldTerminal.setTerminalId("OTHER-TERMINAL");
        paymentFileRepository.saveFinal(oldTerminal);
        paymentService.restoreActivePayment();
        assertThat(paymentService.findPayment("payment-1").getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
        assertThatThrownBy(() -> paymentService.cancelPayment("payment-1")).isInstanceOf(PinpadPaymentException.class);
        assertThatThrownBy(() -> paymentService.registerPayment(request("payment-2"))).isInstanceOf(PinpadPaymentException.class);
        verifyNoInteractions(culqiTerminalClient);
    }

    @Test
    void demoPreservesExistingRegisterSearchAndAckFlow() {
        paymentService.shutdown();
        properties.setProvider(PinpadProvider.DEMO);
        properties.setSimulatorMinDelayMillis(0);
        properties.setSimulatorMaxDelayMillis(0);
        paymentService = new PinpadPaymentService(paymentFileRepository, new PinpadSimulatorAdapter(properties), properties, objectMapper);
        PinpadPaymentRegisterDto request = request("demo-1");
        request.setAmountCents(10001L); // Caso determinista de rechazo del simulador existente.
        assertThat(paymentService.registerPayment(request).getStatus()).isEqualTo(PinpadPaymentStatus.REJECTED);
        assertThat(paymentService.searchPayment("demo-1").orElseThrow().getVoucher()).contains("SIMULADO");
        assertThat(paymentService.ackPayment("demo-1", null).getStatus()).isEqualTo(PinpadPaymentStatus.READ);
        assertThat(paymentService.health().getProvider()).isEqualTo("demo");
    }

    @Test
    void niubizUnknownSurvivesRestartAndIsReconciledWithoutChargingAgain() {
        NiubizTerminalClient niubizTerminalClient = useNiubiz();
        when(niubizTerminalClient.processPayment(any())).thenThrow(new IllegalStateException("transport failure"));
        assertThat(paymentService.registerPayment(request("niubiz-1")).getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
        paymentService.shutdown();
        paymentService = new PinpadPaymentService(paymentFileRepository, new PinpadNiubizAdapter(properties, niubizTerminalClient),
                properties, objectMapper);
        paymentService.restoreActivePayment();
        assertThat(paymentService.health().getProvider()).isEqualTo("niubiz");
        assertThat(paymentService.health().getActivePaymentId()).isEqualTo("niubiz-1");
        assertThatThrownBy(() -> paymentService.registerPayment(request("niubiz-2"))).isInstanceOf(PinpadPaymentException.class);
        assertThatThrownBy(() -> paymentService.ackPayment("niubiz-1", null)).isInstanceOf(PinpadPaymentException.class);
        when(niubizTerminalClient.queryPayment("niubiz-1")).thenReturn(Optional.of(approved()));
        assertThat(paymentService.findPayment("niubiz-1").getStatus()).isEqualTo(PinpadPaymentStatus.APPROVED);
        assertThat(Files.exists(storage.resolve("unknown/niubiz-1.json"))).isFalse();
        assertThat(paymentService.ackPayment("niubiz-1", null).getStatus()).isEqualTo(PinpadPaymentStatus.READ);
        verify(niubizTerminalClient, times(1)).processPayment(any());
    }

    @Test
    void niubizTimeoutBlocksNewChargesUntilTheOriginalPaymentIsResolved() {
        NiubizTerminalClient niubizTerminalClient = useNiubiz();
        PinpadPaymentDetailDto interrupted = persistedPayment("niubiz-1", PinpadPaymentStatus.PROCESSING);
        interrupted.setStartedAt(LocalDateTime.now().minusMinutes(10));
        paymentFileRepository.saveProcessing(interrupted);
        paymentService.restoreActivePayment();
        assertThat(paymentService.findPayment("niubiz-1").getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
        assertThatThrownBy(() -> paymentService.registerPayment(request("niubiz-2"))).isInstanceOf(PinpadPaymentException.class);
        when(niubizTerminalClient.cancelPayment("niubiz-1")).thenReturn(approved());
        assertThat(paymentService.cancelPayment("niubiz-1").getStatus()).isEqualTo(PinpadPaymentStatus.APPROVED);
        verify(niubizTerminalClient, never()).processPayment(any());
    }

    @Test
    void niubizCannotQueryCancelOrReuseCulqiResults() {
        paymentFileRepository.saveFinal(persistedPayment("culqi-1", PinpadPaymentStatus.UNKNOWN));
        NiubizTerminalClient niubizTerminalClient = useNiubiz();
        paymentService.restoreActivePayment();
        assertThat(paymentService.findPayment("culqi-1").getStatus()).isEqualTo(PinpadPaymentStatus.UNKNOWN);
        assertThatThrownBy(() -> paymentService.cancelPayment("culqi-1")).isInstanceOf(PinpadPaymentException.class);
        assertThatThrownBy(() -> paymentService.registerPayment(request("culqi-1"))).isInstanceOf(PinpadPaymentException.class);
        assertThatThrownBy(() -> paymentService.registerPayment(request("niubiz-1"))).isInstanceOf(PinpadPaymentException.class);
        verifyNoInteractions(niubizTerminalClient);
    }

    @Test
    void switchingToCulqiCannotHideUncertainNiubizPayment() {
        useNiubiz();
        paymentFileRepository.saveFinal(persistedPayment("niubiz-1", PinpadPaymentStatus.UNKNOWN));
        paymentService.shutdown();
        properties.setProvider(PinpadProvider.CULQI);
        paymentService = createService();
        paymentService.restoreActivePayment();
        assertThatThrownBy(() -> paymentService.registerPayment(request("culqi-1"))).isInstanceOf(PinpadPaymentException.class);
        assertThatThrownBy(() -> paymentService.ackPayment("niubiz-1", null)).isInstanceOf(PinpadPaymentException.class);
        verifyNoInteractions(culqiTerminalClient);
    }

    @Test
    void referenceCannotBeClaimedByAnotherCashierOrInternalPaymentMethod() {
        when(culqiTerminalClient.processPayment(any())).thenReturn(approved());
        PinpadPaymentRegisterDto original = request("reference-owner");
        original.setCashier("USER1");
        original.setInternalPaymentCode("TC001");
        paymentService.registerPayment(original);
        PinpadPaymentRegisterDto changed = request("reference-owner");
        changed.setCashier("USER2");
        changed.setInternalPaymentCode("TC001");
        assertThatThrownBy(() -> paymentService.registerPayment(changed)).isInstanceOf(PinpadPaymentException.class);
        changed.setCashier("USER1");
        changed.setInternalPaymentCode("TD001");
        assertThatThrownBy(() -> paymentService.registerPayment(changed)).isInstanceOf(PinpadPaymentException.class);
        verify(culqiTerminalClient, times(1)).processPayment(any());
    }

    private NiubizTerminalClient useNiubiz() {
        paymentService.shutdown();
        properties.setProvider(PinpadProvider.NIUBIZ);
        NiubizTerminalClient niubizTerminalClient = mock(NiubizTerminalClient.class);
        paymentService = new PinpadPaymentService(paymentFileRepository, new PinpadNiubizAdapter(properties, niubizTerminalClient),
                properties, objectMapper);
        return niubizTerminalClient;
    }

    private PinpadPaymentService createService() {
        return new PinpadPaymentService(paymentFileRepository, new PinpadCulqiAdapter(properties, culqiTerminalClient),
                properties, objectMapper);
    }

    private PinpadPaymentRegisterDto request(String paymentId) {
        PinpadPaymentRegisterDto request = new PinpadPaymentRegisterDto();
        request.setPaymentId(paymentId);
        request.setAmountCents(12345L);
        request.setCurrency("PEN");
        request.setPaymentMethod(PinpadPaymentMethod.CARD);
        return request;
    }

    private PinpadPaymentDetailDto persistedPayment(String paymentId, PinpadPaymentStatus status) {
        PinpadPaymentDetailDto detail = new PinpadPaymentDetailDto();
        detail.setPaymentId(paymentId);
        detail.setProvider(properties.getProvider());
        detail.setTerminalId(properties.getTerminalId());
        detail.setMerchantId(properties.getMerchantId());
        detail.setStatus(status);
        detail.setStartedAt(LocalDateTime.now());
        detail.setAmountCents(12345L);
        detail.setCurrency("PEN");
        detail.setPaymentMethod(PinpadPaymentMethod.CARD);
        return detail;
    }

    private PinpadAdapterResult approved() {
        PinpadAdapterResult result = result(PinpadPaymentStatus.APPROVED);
        result.setTransactionId("culqi-transaction");
        return result;
    }

    private PinpadAdapterResult result(PinpadPaymentStatus status) {
        PinpadAdapterResult result = new PinpadAdapterResult();
        result.setStatus(status);
        return result;
    }
}
