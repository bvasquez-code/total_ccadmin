package com.ccadmin.app.producttraceability.event;

import com.ccadmin.app.inventory.model.constants.StockMovementConstants;
import com.ccadmin.app.producttraceability.exception.PendingProductTraceabilityException;
import com.ccadmin.app.producttraceability.service.ProductTraceabilityTaskService;
import com.ccadmin.app.pucharse.model.constants.PucharseConstants;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class ProductTraceabilityEventListener {

    private static final int MAX_ATTEMPTS = 6;
    private static final long INITIAL_RETRY_DELAY_MILLIS = 100L;

    private final ProductTraceabilityTaskService productTraceabilityTaskService;
    private final ExecutorService executorService;
    private final ConcurrentHashMap<String, CompletableFuture<Void>> taskByStore =
            new ConcurrentHashMap<>();

    public ProductTraceabilityEventListener(
            ProductTraceabilityTaskService productTraceabilityTaskService,
            @Qualifier("productTraceabilityExecutor") ExecutorService executorService
    ) {
        this.productTraceabilityTaskService = productTraceabilityTaskService;
        this.executorService = executorService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(ProductTraceabilityConfirmedOperationEvent event) {
        try {
            this.enqueueByStore(event);
        } catch (RuntimeException exception) {
            log.error(
                    "TRAZABILIDAD_NO_ENCOLADA -->> {} | {} | {}",
                    event.sourceTable(), event.operationCode(), event.storeCode(), exception
            );
        }
    }

    private void enqueueByStore(ProductTraceabilityConfirmedOperationEvent event) {
        this.taskByStore.compute(event.storeCode(), (storeCode, currentTask) -> {
            CompletableFuture<Void> previousTask = currentTask == null
                    ? CompletableFuture.completedFuture(null)
                    : currentTask.handle((ignored, exception) -> null);
            CompletableFuture<Void> nextTask = previousTask.thenRunAsync(
                    () -> this.processWithRetry(event),
                    this.executorService
            );
            nextTask.whenComplete((ignored, exception) -> this.taskByStore.compute(
                    storeCode,
                    (key, registeredTask) -> registeredTask == nextTask ? null : registeredTask
            ));
            return nextTask;
        });
    }

    private void processWithRetry(ProductTraceabilityConfirmedOperationEvent event) {
        long retryDelay = INITIAL_RETRY_DELAY_MILLIS;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                this.process(event);
                return;
            } catch (PendingProductTraceabilityException exception) {
                if (attempt == MAX_ATTEMPTS) {
                    this.logFailure(event, exception);
                    return;
                }
                if (!this.waitForRetry(retryDelay)) {
                    return;
                }
                retryDelay *= 2;
            } catch (Exception exception) {
                this.logFailure(event, exception);
                return;
            }
        }
    }

    private void process(ProductTraceabilityConfirmedOperationEvent event) {
        switch (event.sourceTable()) {
            case PucharseConstants.KARDEX_ZONE_SOURCE ->
                    this.productTraceabilityTaskService.processPurchase(
                            event.operationCode(), event.storeCode()
                    );
            case SaleConstants.KARDEX_ZONE_SOURCE_SALE ->
                    this.productTraceabilityTaskService.processSale(
                            event.operationCode(), event.storeCode()
                    );
            case SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE ->
                    this.productTraceabilityTaskService.processCreditNote(
                            event.operationCode(), event.storeCode()
                    );
            case TransferConstants.KARDEX_SOURCE_TABLE ->
                    this.productTraceabilityTaskService.processTransfer(
                            event.operationCode(), event.storeCode()
                    );
            case TransferConstants.KARDEX_ZONE_SOURCE_REQUEST ->
                    this.productTraceabilityTaskService.processTransferRequest(
                            event.operationCode(), event.storeCode()
                    );
            case StockMovementConstants.SOURCE_ENTRY ->
                    this.productTraceabilityTaskService.processStockEntry(
                            event.operationCode(), event.storeCode()
                    );
            case StockMovementConstants.SOURCE_EXIT ->
                    this.productTraceabilityTaskService.processStockExit(
                            event.operationCode(), event.storeCode()
                    );
            default -> log.debug(
                    "TRAZABILIDAD_ORIGEN_NO_SOPORTADO -->> {} | {}",
                    event.sourceTable(), event.operationCode()
            );
        }
    }

    private boolean waitForRetry(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void logFailure(
            ProductTraceabilityConfirmedOperationEvent event,
            Exception exception
    ) {
        log.error(
                "ERROR_TRAZABILIDAD_PRODUCTO -->> {} | {} | {}: {}",
                event.sourceTable(),
                event.operationCode(),
                event.storeCode(),
                exception.getMessage(),
                exception
        );
    }
}
