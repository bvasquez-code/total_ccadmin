package com.local.app.pinpad.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.local.app.pinpad.adapter.PinpadAdapter;
import com.local.app.pinpad.adapter.PinpadAdapterResult;
import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.constants.PinpadConstants;
import com.local.app.pinpad.enums.PinpadErrorCode;
import com.local.app.pinpad.enums.PinpadPaymentStatus;
import com.local.app.pinpad.exception.PinpadPaymentException;
import com.local.app.pinpad.model.dto.PinpadPaymentAckDto;
import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;
import com.local.app.pinpad.model.dto.PinpadPaymentHealthDto;
import com.local.app.pinpad.model.dto.PinpadPaymentRegisterDto;
import com.local.app.pinpad.repository.PinpadPaymentFileRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PinpadPaymentService {

    private final PinpadPaymentFileRepository repository;
    private final PinpadAdapter pinpadAdapter;
    private final PinpadAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final AtomicReference<String> activePaymentId = new AtomicReference<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public PinpadPaymentService(PinpadPaymentFileRepository repository,
                                PinpadAdapter pinpadAdapter,
                                PinpadAgentProperties properties,
                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.pinpadAdapter = pinpadAdapter;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void restoreActivePayment() {
        repository.findFirstProcessing().ifPresent(payment -> activePaymentId.set(payment.getPaymentId()));
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }

    public PinpadPaymentDetailDto registerPayment(PinpadPaymentRegisterDto request) {
        validateRequest(request);
        Optional<PinpadPaymentDetailDto> existing = repository.findByPaymentId(request.getPaymentId());
        if (existing.isPresent()) {
            assertSameIdempotencyData(existing.get(), request);
            return refreshTimeoutIfNeeded(existing.get());
        }

        String currentActive = resolveActivePaymentId();
        if (currentActive != null && !currentActive.equals(request.getPaymentId())) {
            throw new PinpadPaymentException(PinpadErrorCode.PINPAD_BUSY,
                    "El pinpad ya tiene una operacion en proceso", HttpStatus.CONFLICT);
        }
        if (!activePaymentId.compareAndSet(null, request.getPaymentId())) {
            throw new PinpadPaymentException(PinpadErrorCode.PINPAD_BUSY,
                    "El pinpad ya tiene una operacion en proceso", HttpStatus.CONFLICT);
        }

        PinpadPaymentDetailDto detail = buildProcessingPayment(request);
        try {
            repository.saveProcessing(detail);
            repository.appendLog(detail.getPaymentId(), "PAYMENT_CREATED", request);
            Future<?> future = executorService.submit(() -> processAsync(detail.getPaymentId()));
            return waitFinalResultIfConfigured(detail, future);
        } catch (RuntimeException ex) {
            activePaymentId.compareAndSet(request.getPaymentId(), null);
            throw ex;
        }
    }

    public PinpadPaymentDetailDto findPayment(String paymentId) {
        return repository.findByPaymentId(paymentId)
                .map(this::refreshTimeoutIfNeeded)
                .orElseThrow(() -> new PinpadPaymentException(PinpadErrorCode.PAYMENT_NOT_FOUND, "Pago no encontrado", HttpStatus.NOT_FOUND));
    }

    public Optional<PinpadPaymentDetailDto> searchPayment(String paymentId) {
        repository.validatePaymentId(paymentId);
        return repository.findInAllFolders(paymentId).map(this::refreshTimeoutIfNeeded);
    }

    public PinpadPaymentDetailDto ackPayment(String paymentId, PinpadPaymentAckDto ackDto) {
        PinpadPaymentDetailDto detail = findPayment(paymentId);
        if (detail.getStatus() == PinpadPaymentStatus.PROCESSING) {
            throw new PinpadPaymentException(PinpadErrorCode.INVALID_PAYMENT_STATUS,
                    "No se puede confirmar lectura mientras el pago esta en proceso", HttpStatus.CONFLICT);
        }
        if (!detail.getStatus().isFinalBeforeRead() && detail.getStatus() != PinpadPaymentStatus.READ) {
            throw new PinpadPaymentException(PinpadErrorCode.INVALID_PAYMENT_STATUS,
                    "El pago no esta en un estado final valido para ACK", HttpStatus.CONFLICT);
        }
        if (detail.getStatus() == PinpadPaymentStatus.READ) {
            return detail;
        }
        return repository.markAsRead(paymentId, ackDto == null ? new PinpadPaymentAckDto() : ackDto);
    }

    public PinpadPaymentDetailDto cancelPayment(String paymentId) {
        PinpadPaymentDetailDto detail = findPayment(paymentId);
        if (detail.getStatus() != PinpadPaymentStatus.PROCESSING) {
            return detail;
        }
        PinpadAdapterResult result = pinpadAdapter.cancelPayment(paymentId);
        applyAdapterResult(detail, result);
        detail.setStatus(PinpadPaymentStatus.CANCELLED);
        detail.setFinishedAt(LocalDateTime.now());
        detail.setUpdatedAt(LocalDateTime.now());
        repository.saveFinal(detail);
        repository.appendLog(paymentId, "PAYMENT_CANCELLED", result);
        activePaymentId.compareAndSet(paymentId, null);
        return detail;
    }

    public PinpadPaymentHealthDto health() {
        PinpadPaymentHealthDto dto = new PinpadPaymentHealthDto();
        dto.setStatus("UP");
        dto.setAgentVersion(PinpadConstants.AGENT_VERSION);
        dto.setPinpadStatus(pinpadAdapter.getPinpadStatus());
        dto.setSimulatorEnabled(properties.isSimulatorEnabled());
        dto.setTerminalId(properties.getTerminalId());
        dto.setMerchantId(properties.getMerchantId());
        dto.setActivePaymentId(resolveActivePaymentId());
        return dto;
    }

    private void processAsync(String paymentId) {
        try {
            PinpadPaymentDetailDto current = repository.findByPaymentId(paymentId).orElse(null);
            if (current == null || current.getStatus() != PinpadPaymentStatus.PROCESSING) {
                return;
            }
            PinpadAdapterResult result = pinpadAdapter.processPayment(current);
            PinpadPaymentDetailDto afterAdapter = repository.findByPaymentId(paymentId).orElse(current);
            if (afterAdapter.getStatus() != PinpadPaymentStatus.PROCESSING) {
                return;
            }
            applyAdapterResult(afterAdapter, result);
            afterAdapter.setFinishedAt(LocalDateTime.now());
            afterAdapter.setUpdatedAt(LocalDateTime.now());
            afterAdapter.setRawResponseJson(toJson(result));
            repository.saveFinal(afterAdapter);
            repository.appendLog(paymentId, "PAYMENT_FINISHED", result);
        } catch (Exception ex) {
            saveAsyncError(paymentId, ex);
        } finally {
            activePaymentId.compareAndSet(paymentId, null);
        }
    }

    private PinpadPaymentDetailDto waitFinalResultIfConfigured(PinpadPaymentDetailDto detail, Future<?> future) {
        if (!properties.isWaitFinalResultOnRegister()) {
            return detail;
        }
        try {
            future.get(properties.getRegisterWaitTimeoutSeconds(), TimeUnit.SECONDS);
            return repository.findByPaymentId(detail.getPaymentId()).orElse(detail);
        } catch (TimeoutException ex) {
            return detail;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return detail;
        } catch (ExecutionException ex) {
            return repository.findByPaymentId(detail.getPaymentId()).orElse(detail);
        }
    }

    private void saveAsyncError(String paymentId, Exception ex) {
        repository.findByPaymentId(paymentId).ifPresent(detail -> {
            detail.setStatus(PinpadPaymentStatus.ERROR);
            detail.setMessage("Error procesando pago");
            detail.setErrorCode(PinpadErrorCode.PINPAD_ERROR.name());
            detail.setErrorMessage(ex.getMessage());
            detail.setFinishedAt(LocalDateTime.now());
            detail.setUpdatedAt(LocalDateTime.now());
            repository.saveFinal(detail);
            repository.appendLog(paymentId, "PAYMENT_ERROR", ex.getMessage());
        });
    }

    private PinpadPaymentDetailDto refreshTimeoutIfNeeded(PinpadPaymentDetailDto detail) {
        if (detail.getStatus() != PinpadPaymentStatus.PROCESSING || detail.getStartedAt() == null) {
            return detail;
        }
        LocalDateTime timeoutAt = detail.getStartedAt().plusSeconds(properties.getTimeoutSeconds());
        detail.setTimeoutAt(timeoutAt);
        if (LocalDateTime.now().isAfter(timeoutAt)) {
            detail.setStatus(PinpadPaymentStatus.TIMEOUT);
            detail.setMessage("Timeout esperando respuesta del pinpad");
            detail.setErrorCode(PinpadErrorCode.PINPAD_TIMEOUT.name());
            detail.setErrorMessage("Operacion supero el timeout configurado");
            detail.setFinishedAt(LocalDateTime.now());
            detail.setUpdatedAt(LocalDateTime.now());
            repository.saveFinal(detail);
            repository.appendLog(detail.getPaymentId(), "PAYMENT_TIMEOUT_REFRESH", detail);
            activePaymentId.compareAndSet(detail.getPaymentId(), null);
        }
        return detail;
    }

    private String resolveActivePaymentId() {
        String current = activePaymentId.get();
        if (current != null) {
            Optional<PinpadPaymentDetailDto> currentPayment = repository.findByPaymentId(current);
            if (currentPayment.isPresent() && currentPayment.get().getStatus() == PinpadPaymentStatus.PROCESSING) {
                refreshTimeoutIfNeeded(currentPayment.get());
                return activePaymentId.get();
            }
            activePaymentId.compareAndSet(current, null);
        }
        Optional<PinpadPaymentDetailDto> processing = repository.findFirstProcessing();
        if (processing.isPresent()) {
            PinpadPaymentDetailDto refreshed = refreshTimeoutIfNeeded(processing.get());
            if (refreshed.getStatus() == PinpadPaymentStatus.PROCESSING) {
                activePaymentId.compareAndSet(null, refreshed.getPaymentId());
                return refreshed.getPaymentId();
            }
        }
        return null;
    }

    private PinpadPaymentDetailDto buildProcessingPayment(PinpadPaymentRegisterDto request) {
        LocalDateTime now = LocalDateTime.now();
        PinpadPaymentDetailDto detail = new PinpadPaymentDetailDto();
        detail.setPaymentId(request.getPaymentId());
        detail.setSaleCod(request.getSaleCod());
        detail.setAmount(request.getAmount());
        detail.setAmountCents(request.getAmountCents());
        detail.setCurrency(request.getCurrency().toUpperCase());
        detail.setPaymentMethod(request.getPaymentMethod());
        detail.setInternalPaymentCode(request.getInternalPaymentCode());
        detail.setCashier(request.getCashier());
        detail.setStoreCod(request.getStoreCod());
        detail.setTerminalCod(request.getTerminalCod());
        detail.setExternalReference(request.getExternalReference());
        detail.setStatus(PinpadPaymentStatus.PROCESSING);
        detail.setMessage("Pago enviado al pinpad");
        detail.setCreatedAt(now);
        detail.setUpdatedAt(now);
        detail.setStartedAt(now);
        detail.setTimeoutAt(now.plusSeconds(properties.getTimeoutSeconds()));
        detail.setAckReceived(false);
        detail.setCentralSaved(false);
        detail.setRawRequestJson(toJson(request));
        return detail;
    }

    private void validateRequest(PinpadPaymentRegisterDto request) {
        if (request == null) {
            throw new PinpadPaymentException(PinpadErrorCode.INVALID_REQUEST, "Solicitud requerida");
        }
        repository.validatePaymentId(request.getPaymentId());
        if (request.getAmountCents() == null || request.getAmountCents() <= 0) {
            throw new PinpadPaymentException(PinpadErrorCode.INVALID_REQUEST, "amountCents debe ser mayor a cero");
        }
        if (request.getCurrency() == null || !properties.getAllowedCurrency().equalsIgnoreCase(request.getCurrency())) {
            throw new PinpadPaymentException(PinpadErrorCode.INVALID_REQUEST,
                    "Moneda no permitida para el agente pinpad");
        }
        if (request.getPaymentMethod() == null) {
            throw new PinpadPaymentException(PinpadErrorCode.INVALID_REQUEST, "paymentMethod es obligatorio");
        }
    }

    private void assertSameIdempotencyData(PinpadPaymentDetailDto existing, PinpadPaymentRegisterDto request) {
        if (!Objects.equals(existing.getAmountCents(), request.getAmountCents())
                || !Objects.equals(existing.getCurrency(), request.getCurrency().toUpperCase())
                || existing.getPaymentMethod() != request.getPaymentMethod()) {
            throw new PinpadPaymentException(PinpadErrorCode.IDEMPOTENCY_AMOUNT_MISMATCH,
                    "paymentId existente con monto, moneda o medio de pago distinto", HttpStatus.CONFLICT);
        }
    }

    private void applyAdapterResult(PinpadPaymentDetailDto detail, PinpadAdapterResult result) {
        detail.setStatus(result.getStatus());
        detail.setMessage(result.getMessage());
        detail.setErrorCode(result.getErrorCode());
        detail.setErrorMessage(result.getErrorMessage());
        detail.setTransactionId(result.getTransactionId());
        detail.setAuthorizationCode(result.getAuthorizationCode());
        detail.setReferenceNumber(result.getReferenceNumber());
        detail.setTerminalId(result.getTerminalId());
        detail.setMerchantId(result.getMerchantId());
        detail.setCardBrand(result.getCardBrand());
        detail.setCardType(result.getCardType());
        detail.setLastFour(result.getLastFour());
        detail.setWalletName(result.getWalletName());
        detail.setPaymentMethodDescription(result.getPaymentMethodDescription());
        detail.setVoucher(result.getVoucher());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
