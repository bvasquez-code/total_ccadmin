package com.ccadmin.app.delivery.model.dto;

public class MercadoPagoRequestDto extends SaleDeliveryAccessRequestDto {
    public String PaymentMethodCod;
    public String AttemptId;
    public CardData FormData;

    // Only tokenized fields are accepted. PAN, CVV and client-supplied amounts are never forwarded.
    public static class CardData {
        public String token;
        public String payment_method_id;
        public String issuer_id;
        public Integer installments;
        public Payer payer;
    }
    public static class Payer {
        public String email;
        public Identification identification;
    }
    public static class Identification {
        public String type;
        public String number;
    }
}
