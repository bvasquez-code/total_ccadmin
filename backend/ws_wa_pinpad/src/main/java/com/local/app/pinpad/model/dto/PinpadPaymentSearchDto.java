package com.local.app.pinpad.model.dto;

public class PinpadPaymentSearchDto {

    private boolean found;
    private String paymentId;
    private String message;
    private PinpadPaymentDetailDto payment;

    public static PinpadPaymentSearchDto found(PinpadPaymentDetailDto payment) {
        PinpadPaymentSearchDto dto = new PinpadPaymentSearchDto();
        dto.setFound(true);
        dto.setPaymentId(payment.getPaymentId());
        dto.setPayment(payment);
        return dto;
    }

    public static PinpadPaymentSearchDto notFound(String paymentId) {
        PinpadPaymentSearchDto dto = new PinpadPaymentSearchDto();
        dto.setFound(false);
        dto.setPaymentId(paymentId);
        dto.setMessage("No se encontro operacion local para el paymentId indicado");
        return dto;
    }

    public boolean isFound() { return found; }
    public void setFound(boolean found) { this.found = found; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public PinpadPaymentDetailDto getPayment() { return payment; }
    public void setPayment(PinpadPaymentDetailDto payment) { this.payment = payment; }
}
