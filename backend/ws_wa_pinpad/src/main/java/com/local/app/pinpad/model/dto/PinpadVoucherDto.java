package com.local.app.pinpad.model.dto;

public class PinpadVoucherDto {

    private String paymentId;
    private String voucher;
    private String message;

    public PinpadVoucherDto() {
    }

    public PinpadVoucherDto(String paymentId, String voucher, String message) {
        this.paymentId = paymentId;
        this.voucher = voucher;
        this.message = message;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getVoucher() { return voucher; }
    public void setVoucher(String voucher) { this.voucher = voucher; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
