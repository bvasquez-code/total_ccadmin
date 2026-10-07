package com.local.app.pinpad.adapter.culqi;

import com.local.app.pinpad.adapter.PinpadAdapterResult;

import java.util.Optional;

/**
 * Contrato INTERNO del agente, no es un SDK ni un protocolo oficial de Culqi.
 * Implementar como bean Spring cuando Culqi entregue el SDK/protocolo del terminal.
 * No persistir ni devolver PAN completo, PIN, CVV, pistas ni respuestas crudas del SDK.
 * Todas las llamadas deben tener timeout finito. processPayment no debe reintentarse
 * automaticamente; paymentId debe conservarse como referencia consultable del proveedor.
 * queryPayment y cancelPayment deben poder ejecutarse durante processPayment.
 */
public interface CulqiTerminalClient {

    PinpadAdapterResult processPayment(CulqiTerminalPaymentRequest payment);

    // Solo devolver CANCELLED cuando el terminal confirme que no hubo cobro.
    // Si ya fue cobrado, devolver APPROVED; cancelar no equivale a devolver dinero.
    PinpadAdapterResult cancelPayment(String paymentId);

    // Vacio significa sin resultado confirmado, nunca rechazo ni cancelacion implicitos.
    Optional<PinpadAdapterResult> queryPayment(String paymentId);

    boolean isConnected();
}
