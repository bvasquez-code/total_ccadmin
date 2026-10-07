package com.local.app.pinpad.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.local.app.pinpad.adapter.PinpadAdapter;
import com.local.app.pinpad.adapter.PinpadAdapterResult;
import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.constants.PinpadConstants;
import com.local.app.pinpad.enums.PinpadErrorCode;
import com.local.app.pinpad.enums.PinpadPaymentStatus;
import com.local.app.pinpad.enums.PinpadProvider;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
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
    private final AtomicReference<String> executingPaymentId = new AtomicReference<>();
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
        repository.findFirstUnresolved().ifPresent(payment -> activePaymentId.set(payment.getPaymentId()));
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }

    public PinpadPaymentDetailDto registerPayment(PinpadPaymentRegisterDto request) {
        PinpadPaymentDetailDto detail;
        Future<?> future;
        synchronized (this) {
            validateRequest(request);
            Optional<PinpadPaymentDetailDto> existing = repository.findByPaymentId(request.getPaymentId());
            if (existing.isPresent()) {
                assertSameIdempotencyData(existing.get(), request);
                return refreshTimeoutIfNeeded(existing.get());
            }

            pinpadAdapter.validatePayment(request);
            String currentActive = resolveActivePaymentId();
            if (currentActive != null && !currentActive.equals(request.getPaymentId())) {
                throw new PinpadPaymentException(PinpadErrorCode.PINPAD_BUSY,
                        "El pinpad ya tiene una operacion en proceso", HttpStatus.CONFLICT);
            }
            if (!activePaymentId.compareAndSet(null, request.getPaymentId())) {
                throw new PinpadPaymentException(PinpadErrorCode.PINPAD_BUSY,
                        "El pinpad ya tiene una operacion en proceso", HttpStatus.CONFLICT);
            }

            detail = buildProcessingPayment(request);
            try {
                repository.saveProcessing(detail);
                repository.appendLog(detail.getPaymentId(), "PAYMENT_CREATED", request);
                executingPaymentId.set(detail.getPaymentId());
                future = executorService.submit(() -> processAsync(detail.getPaymentId()));
            } catch (RuntimeException ex) {
                executingPaymentId.compareAndSet(request.getPaymentId(), null);
                activePaymentId.compareAndSet(request.getPaymentId(), null);
                throw ex;
            }
        }
        return waitFinalResultIfConfigured(detail, future);
    }

    public synchronized PinpadPaymentDetailDto findPayment(String paymentId) {
        return repository.findByPaymentId(paymentId)
                .map(this::refreshTimeoutIfNeeded)
                .orElseThrow(() -> new PinpadPaymentException(PinpadErrorCode.PAYMENT_NOT_FOUND, "Pago no encontrado", HttpStatus.NOT_FOUND));
    }

    public synchronized Optional<PinpadPaymentDetailDto> searchPayment(String paymentId) {
        repository.validatePaymentId(paymentId);
        return repository.findInAllFolders(paymentId).map(this::refreshTimeoutIfNeeded);
    }

    public synchronized PinpadPaymentDetailDto ackPayment(String paymentId, PinpadPaymentAckDto ackDto) {
        PinpadPaymentDetailDto detail = findPayment(paymentId);
        if (isUnresolved(detail)) {
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

    public synchronized PinpadPaymentDetailDto cancelPayment(String paymentId) {
        PinpadPaymentDetailDto detail = findPayment(paymentId);
        if (!isUnresolved(detail)) {
            return detail;
        }
        if (!usesCurrentTerminal(detail)) {
            throw new PinpadPaymentException(PinpadErrorCode.INVALID_PAYMENT_STATUS,
                    "El pago pertenece a otro proveedor o terminal; restaurar su configuracion para conciliar", HttpStatus.CONFLICT);
        }
        PinpadAdapterResult result = pinpadAdapter.cancelPayment(paymentId);
        return persistAdapterResult(paymentId, result, "PAYMENT_CANCEL_RESPONSE");
    }

    public synchronized PinpadPaymentHealthDto health() {
        PinpadPaymentHealthDto dto = new PinpadPaymentHealthDto();
        dto.setStatus("UP");
        dto.setAgentVersion(PinpadConstants.AGENT_VERSION);
        dto.setPinpadStatus(pinpadAdapter.getPinpadStatus());
        dto.setSimulatorEnabled(properties.isSimulatorEnabled());
        dto.setProvider(properties.getProvider().name().toLowerCase(Locale.ROOT));
        dto.setTerminalId(properties.getTerminalId());
        dto.setMerchantId(properties.getMerchantId());
        dto.setActivePaymentId(resolveActivePaymentId());
        return dto;
    }

    private void processAsync(String paymentId) {
        try {
            PinpadPaymentDetailDto current;
            synchronized (this) {
                current = repository.findByPaymentId(paymentId).orElse(null);
            }
            if (current == null || current.getStatus() != PinpadPaymentStatus.PROCESSING) {
                return;
            }
            PinpadAdapterResult result = pinpadAdapter.processPayment(current);
            persistAdapterResult(paymentId, result, "PAYMENT_FINISHED");
        } catch (Exception ex) {
            saveAsyncError(paymentId);
        } finally {
            synchronized (this) {
                executingPaymentId.compareAndSet(paymentId, null);
                repository.findByPaymentId(paymentId).filter(payment -> !isUnresolved(payment))
                        .ifPresent(payment -> activePaymentId.compareAndSet(paymentId, null));
            }
        }
    }

    private synchronized PinpadPaymentDetailDto persistAdapterResult(String paymentId, PinpadAdapterResult result,
                                                                     String event) {
        PinpadPaymentDetailDto detail = repository.findByPaymentId(paymentId).orElseThrow();
        // Una respuesta tardia nunca debe sobrescribir un resultado confirmado ni su ACK.
        if (!isUnresolved(detail)) {
            return detail;
        }
        applyAdapterResult(detail, result);
        detail.setFinishedAt(isUnresolved(detail) ? null : LocalDateTime.now());
        detail.setUpdatedAt(LocalDateTime.now());
        detail.setRawResponseJson(toJson(result));
        repository.saveFinal(detail);
        repository.appendLog(paymentId, event, result);
        if (!isUnresolved(detail)) {
            activePaymentId.compareAndSet(paymentId, null);
        }
        return detail;
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

    private synchronized void saveAsyncError(String paymentId) {
        repository.findByPaymentId(paymentId).ifPresent(detail -> {
            if (!isUnresolved(detail)) {
                return;
            }
            PinpadAdapterResult result = new PinpadAdapterResult();
            result.setStatus(pinpadAdapter.requiresReconciliation() ? PinpadPaymentStatus.UNKNOWN : PinpadPaymentStatus.ERROR);
            result.setMessage("No se pudo confirmar el resultado del pago");
            result.setErrorCode(pinpadAdapter.requiresReconciliation()
                    ? PinpadErrorCode.PINPAD_RESULT_UNCONFIRMED.name() : PinpadErrorCode.PINPAD_ERROR.name());
            // No guardar mensajes arbitrarios del SDK que podrian contener datos sensibles.
            persistAdapterResult(paymentId, result, "PAYMENT_ERROR");
        });
    }

    private PinpadPaymentDetailDto refreshTimeoutIfNeeded(PinpadPaymentDetailDto detail) {
        if (isUnresolved(detail) && usesCurrentTerminal(detail) && pinpadAdapter.requiresReconciliation()) {
            boolean expired = detail.getStartedAt() != null
                    && LocalDateTime.now().isAfter(detail.getStartedAt().plusSeconds(properties.getTimeoutSeconds()));
            if (detail.getStatus() == PinpadPaymentStatus.UNKNOWN || expired
                    || !detail.getPaymentId().equals(executingPaymentId.get())) {
                Optional<PinpadAdapterResult> result = pinpadAdapter.queryPayment(detail.getPaymentId());
                if (result.isPresent()) {
                    detail = persistAdapterResult(detail.getPaymentId(), result.get(), "PAYMENT_RECONCILED");
                }
            }
        }
        if (detail.getStatus() != PinpadPaymentStatus.PROCESSING || detail.getStartedAt() == null) {
            return detail;
        }
        LocalDateTime timeoutAt = detail.getStartedAt().plusSeconds(properties.getTimeoutSeconds());
        detail.setTimeoutAt(timeoutAt);
        if (LocalDateTime.now().isAfter(timeoutAt)) {
            PinpadAdapterResult result = new PinpadAdapterResult();
            boolean uncertain = isRealPayment(detail);
            result.setStatus(uncertain ? PinpadPaymentStatus.UNKNOWN : PinpadPaymentStatus.TIMEOUT);
            result.setMessage(uncertain ? "Resultado pendiente de conciliacion; no repetir el cobro"
                    : "Timeout esperando respuesta del pinpad");
            result.setErrorCode(uncertain ? PinpadErrorCode.PINPAD_RESULT_UNCONFIRMED.name()
                    : PinpadErrorCode.PINPAD_TIMEOUT.name());
            detail = persistAdapterResult(detail.getPaymentId(), result, "PAYMENT_TIMEOUT_REFRESH");
        }
        return detail;
    }

    private String resolveActivePaymentId() {
        if (executingPaymentId.get() != null) {
            return executingPaymentId.get();
        }
        String current = activePaymentId.get();
        if (current != null) {
            Optional<PinpadPaymentDetailDto> currentPayment = repository.findByPaymentId(current);
            if (currentPayment.isPresent() && isUnresolved(currentPayment.get())) {
                refreshTimeoutIfNeeded(currentPayment.get());
                return activePaymentId.get();
            }
            activePaymentId.compareAndSet(current, null);
        }
        Optional<PinpadPaymentDetailDto> processing = repository.findFirstUnresolved();
        if (processing.isPresent()) {
            PinpadPaymentDetailDto refreshed = refreshTimeoutIfNeeded(processing.get());
            if (isUnresolved(refreshed)) {
                activePaymentId.compareAndSet(null, refreshed.getPaymentId());
                return refreshed.getPaymentId();
            }
        }
        return null;
    }

    private boolean isRealPayment(PinpadPaymentDetailDto detail) {
        return detail.getProvider() == PinpadProvider.CULQI;
    }

    private boolean isUnresolved(PinpadPaymentDetailDto detail) {
        return detail.getStatus() == PinpadPaymentStatus.PROCESSING
                || (isRealPayment(detail) && detail.getStatus() == PinpadPaymentStatus.UNKNOWN);
    }

    private boolean usesCurrentProvider(PinpadPaymentDetailDto detail) {
        // Los archivos beta sin proveedor corresponden al simulador.
        return (detail.getProvider() == null ? PinpadProvider.DEMO : detail.getProvider())
                == properties.getProvider();
    }

    private boolean usesCurrentTerminal(PinpadPaymentDetailDto detail) {
        return usesCurrentProvider(detail) && (!isRealPayment(detail)
                || (Objects.equals(detail.getTerminalId(), properties.getTerminalId())
                && Objects.equals(detail.getMerchantId(), properties.getMerchantId())));
    }

    private PinpadPaymentDetailDto buildProcessingPayment(PinpadPaymentRegisterDto request) {
        LocalDateTime now = LocalDateTime.now();
        PinpadPaymentDetailDto detail = new PinpadPaymentDetailDto();
        detail.setPaymentId(request.getPaymentId());
        detail.setProvider(properties.getProvider());
        detail.setTerminalId(properties.getTerminalId());
        detail.setMerchantId(properties.getMerchantId());
        detail.setSaleCod(request.getSaleCod());
        detail.setAmount(request.getAmount());
        detail.setAmountCents(request.getAmountCents());
        detail.setCurrency(request.getCurrency().toUpperCase(Locale.ROOT));
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
        if (request.getAmount() != null
                && request.getAmount().movePointRight(2).compareTo(BigDecimal.valueOf(request.getAmountCents())) != 0) {
            throw new PinpadPaymentException(PinpadErrorCode.INVALID_REQUEST, "amount y amountCents no coinciden");
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
        if (!usesCurrentTerminal(existing)) {
            throw new PinpadPaymentException(PinpadErrorCode.INVALID_PAYMENT_STATUS,
                    "paymentId existente de otro proveedor o terminal; usar almacenamiento separado",
                    HttpStatus.CONFLICT);
        }
        if (!Objects.equals(existing.getAmountCents(), request.getAmountCents())
                || !Objects.equals(existing.getCurrency(), request.getCurrency().toUpperCase(Locale.ROOT))
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
        if (result.getTerminalId() != null) {
            detail.setTerminalId(result.getTerminalId());
        }
        if (result.getMerchantId() != null) {
            detail.setMerchantId(result.getMerchantId());
        }
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
