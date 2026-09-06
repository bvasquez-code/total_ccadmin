package com.ccadmin.app.producttraceability.event;

public record ProductTraceabilityConfirmedOperationEvent(
        String sourceTable,
        String operationCode,
        String storeCode
) {
}
