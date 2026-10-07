package com.ccadmin.app.payment.exception;

public class PinpadPaymentException extends IllegalStateException {
    public final String PinpadPaymentId;
    public final String PaymentStatus;
    public final boolean CanStartNewPayment;

    public PinpadPaymentException(String paymentId, String status, String message) {
        super(message);
        this.PinpadPaymentId = paymentId;
        this.PaymentStatus = status;
        this.CanStartNewPayment = "REJECTED".equals(status) || "CANCELLED".equals(status);
    }
}
