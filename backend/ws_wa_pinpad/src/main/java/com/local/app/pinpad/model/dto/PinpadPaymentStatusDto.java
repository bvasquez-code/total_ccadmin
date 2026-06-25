package com.local.app.pinpad.model.dto;

import com.local.app.pinpad.enums.PinpadPaymentStatus;

public class PinpadPaymentStatusDto {

    private String paymentId;
    private String saleCod;
    private PinpadPaymentStatus status;
    private Long amountCents;
    private String currency;
    private String transactionId;
    private String authorizationCode;
    private String referenceNumber;
    private String terminalId;
    private String merchantId;
    private String cardBrand;
    private String cardType;
    private String lastFour;
    private String walletName;
    private String message;

    public static PinpadPaymentStatusDto fromDetail(PinpadPaymentDetailDto detail) {
        PinpadPaymentStatusDto dto = new PinpadPaymentStatusDto();
        dto.setPaymentId(detail.getPaymentId());
        dto.setSaleCod(detail.getSaleCod());
        dto.setStatus(detail.getStatus());
        dto.setAmountCents(detail.getAmountCents());
        dto.setCurrency(detail.getCurrency());
        dto.setTransactionId(detail.getTransactionId());
        dto.setAuthorizationCode(detail.getAuthorizationCode());
        dto.setReferenceNumber(detail.getReferenceNumber());
        dto.setTerminalId(detail.getTerminalId());
        dto.setMerchantId(detail.getMerchantId());
        dto.setCardBrand(detail.getCardBrand());
        dto.setCardType(detail.getCardType());
        dto.setLastFour(detail.getLastFour());
        dto.setWalletName(detail.getWalletName());
        dto.setMessage(detail.getMessage());
        return dto;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getSaleCod() { return saleCod; }
    public void setSaleCod(String saleCod) { this.saleCod = saleCod; }
    public PinpadPaymentStatus getStatus() { return status; }
    public void setStatus(PinpadPaymentStatus status) { this.status = status; }
    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
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
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
