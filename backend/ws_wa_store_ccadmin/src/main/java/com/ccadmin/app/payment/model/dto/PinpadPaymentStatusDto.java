package com.ccadmin.app.payment.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PinpadPaymentStatusDto(
        String paymentId,
        String status,
        Long amountCents,
        String currency,
        String transactionId,
        String authorizationCode,
        String referenceNumber,
        String terminalId,
        String merchantId,
        String cardBrand,
        String cardType,
        String lastFour,
        String cashier,
        String internalPaymentCode
) {}
