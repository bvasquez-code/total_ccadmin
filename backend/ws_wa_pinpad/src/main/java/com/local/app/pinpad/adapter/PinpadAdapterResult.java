package com.local.app.pinpad.adapter;

import com.local.app.pinpad.enums.PinpadPaymentStatus;

public class PinpadAdapterResult {

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
}
