package com.local.app.pinpad.model.dto;

import com.local.app.pinpad.enums.PinpadPaymentMethod;
import com.local.app.pinpad.enums.PinpadPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PinpadPaymentDetailDto {

    private String paymentId;
    private String saleCod;
    private BigDecimal amount;
    private Long amountCents;
    private String currency;
    private PinpadPaymentMethod paymentMethod;
    private String internalPaymentCode;
    private String cashier;
    private String storeCod;
    private String terminalCod;
    private String externalReference;
    private PinpadPaymentStatus status;
    private String message;
    private String errorCode;
    private String errorMessage;
    private String transactionId;
    private String authorizationCode;
    private String referenceNumber;
    private String terminalId;
    private String merchantId;
    private String cardBrand;
    private String cardType;
    private String lastFour;
    private String walletName;
    private String paymentMethodDescription;
    private String voucher;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime timeoutAt;
    private Boolean ackReceived;
    private LocalDateTime ackAt;
    private Boolean centralSaved;
    private String centralPaymentCod;
    private String rawRequestJson;
    private String rawResponseJson;

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getSaleCod() { return saleCod; }
    public void setSaleCod(String saleCod) { this.saleCod = saleCod; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PinpadPaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PinpadPaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getInternalPaymentCode() { return internalPaymentCode; }
    public void setInternalPaymentCode(String internalPaymentCode) { this.internalPaymentCode = internalPaymentCode; }
    public String getCashier() { return cashier; }
    public void setCashier(String cashier) { this.cashier = cashier; }
    public String getStoreCod() { return storeCod; }
    public void setStoreCod(String storeCod) { this.storeCod = storeCod; }
    public String getTerminalCod() { return terminalCod; }
    public void setTerminalCod(String terminalCod) { this.terminalCod = terminalCod; }
    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }
    public PinpadPaymentStatus getStatus() { return status; }
    public void setStatus(PinpadPaymentStatus status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getAuthorizationCode() { return authorizationCode; }
    public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    public String getLastFour() { return lastFour; }
    public void setLastFour(String lastFour) { this.lastFour = lastFour; }
    public String getWalletName() { return walletName; }
    public void setWalletName(String walletName) { this.walletName = walletName; }
    public String getPaymentMethodDescription() { return paymentMethodDescription; }
    public void setPaymentMethodDescription(String paymentMethodDescription) { this.paymentMethodDescription = paymentMethodDescription; }
    public String getVoucher() { return voucher; }
    public void setVoucher(String voucher) { this.voucher = voucher; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public LocalDateTime getTimeoutAt() { return timeoutAt; }
    public void setTimeoutAt(LocalDateTime timeoutAt) { this.timeoutAt = timeoutAt; }
    public Boolean getAckReceived() { return ackReceived; }
    public void setAckReceived(Boolean ackReceived) { this.ackReceived = ackReceived; }
    public LocalDateTime getAckAt() { return ackAt; }
    public void setAckAt(LocalDateTime ackAt) { this.ackAt = ackAt; }
    public Boolean getCentralSaved() { return centralSaved; }
    public void setCentralSaved(Boolean centralSaved) { this.centralSaved = centralSaved; }
    public String getCentralPaymentCod() { return centralPaymentCod; }
    public void setCentralPaymentCod(String centralPaymentCod) { this.centralPaymentCod = centralPaymentCod; }
    public String getRawRequestJson() { return rawRequestJson; }
    public void setRawRequestJson(String rawRequestJson) { this.rawRequestJson = rawRequestJson; }
    public String getRawResponseJson() { return rawResponseJson; }
    public void setRawResponseJson(String rawResponseJson) { this.rawResponseJson = rawResponseJson; }
}
