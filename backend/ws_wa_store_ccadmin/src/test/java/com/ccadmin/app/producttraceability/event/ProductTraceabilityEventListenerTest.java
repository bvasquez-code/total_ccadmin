package com.ccadmin.app.producttraceability.event;

import com.ccadmin.app.producttraceability.service.ProductTraceabilityTaskService;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class ProductTraceabilityEventListenerTest {

    @Test
    void confirmedSaleIsDelegatedOutsideTheCallingThread() {
        ProductTraceabilityTaskService taskService =
                mock(ProductTraceabilityTaskService.class);
        try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            ProductTraceabilityEventListener listener =
                    new ProductTraceabilityEventListener(taskService, executorService);

            listener.afterCommit(new ProductTraceabilityConfirmedOperationEvent(
                    SaleConstants.KARDEX_ZONE_SOURCE_SALE,
                    "V001",
                    "T001"
            ));

            verify(taskService, timeout(1_000)).processSale("V001", "T001");
        }
    }

    @Test
    void listenerIsRestrictedToTheAfterCommitPhase() throws NoSuchMethodException {
        Method method = ProductTraceabilityEventListener.class.getMethod(
                "afterCommit",
                ProductTraceabilityConfirmedOperationEvent.class
        );
        TransactionalEventListener annotation =
                method.getAnnotation(TransactionalEventListener.class);

        assertEquals(TransactionPhase.AFTER_COMMIT, annotation.phase());
        assertEquals(false, annotation.fallbackExecution());
    }
}
