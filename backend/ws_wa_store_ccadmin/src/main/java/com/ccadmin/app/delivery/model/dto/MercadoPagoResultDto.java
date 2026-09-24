package com.ccadmin.app.delivery.model.dto;

import com.ccadmin.app.payment.model.entity.MercadoPagoAttemptEntity;

public class MercadoPagoResultDto {
    public String AttemptId;
    public String PaymentId;
    public String PaymentMethodCod;
    public String State;
    public String ProviderStatus;
    public String ProviderStatusDetail;
    public String Message;

    public static MercadoPagoResultDto from(MercadoPagoAttemptEntity attempt) {
        MercadoPagoResultDto result = new MercadoPagoResultDto();
        result.AttemptId = attempt.AttemptId;
        result.PaymentId = attempt.PaymentId;
        result.PaymentMethodCod = attempt.PaymentMethodCod;
        result.State = attempt.PaymentState;
        result.ProviderStatus = attempt.ProviderStatus;
        result.ProviderStatusDetail = attempt.ProviderStatusDetail;
        result.Message = switch (attempt.PaymentState) {
            case "C" -> "Mercado Pago aprobo el pago. El pedido queda pendiente de confirmacion por la tienda.";
            case "F" -> rejectionMessage(attempt);
            default -> "Estamos verificando el pago con Mercado Pago. Consulta su estado antes de intentar otro pago.";
        };
        return result;
    }

    private static String rejectionMessage(MercadoPagoAttemptEntity attempt) {
        if ("cancelled".equals(attempt.ProviderStatus)) {
            return "El pago fue cancelado en Mercado Pago. Puedes realizar un nuevo intento.";
        }
        if ("cc_rejected_other_reason".equals(attempt.ProviderStatusDetail)) {
            return "Mercado Pago rechazo el pago sin especificar una causa adicional. "
                    + ("S".equals(attempt.TestMode)
                    ? "En modo de prueba, verifica la tarjeta y los datos del escenario: usa APRO como titular para probar la aprobacion."
                    : "Contacta al emisor de tu tarjeta o utiliza otro medio de pago.");
        }
        return "El pago no fue aprobado. Puedes intentar nuevamente con otra tarjeta.";
    }
}
