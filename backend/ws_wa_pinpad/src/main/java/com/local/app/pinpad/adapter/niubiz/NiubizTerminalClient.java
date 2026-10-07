package com.local.app.pinpad.adapter.niubiz;

import com.local.app.pinpad.adapter.PinpadAdapterResult;

import java.util.Optional;

/**
 * Contrato INTERNO del agente, no es un SDK ni un protocolo oficial de Niubiz.
 * Implementar como bean Spring con el contrato presencial autorizado para el modelo contratado.
 * El transporte puede ser HTTPS hacia una plataforma cloud; no requiere necesariamente un SDK nativo.
 * No asumir endpoints, autenticacion ni que terminalId sea el numero de serie del equipo.
 * No utilizar la API de ecommerce para controlar el equipo fisico.
 * Todas las llamadas deben tener timeout finito. No reintentar automaticamente una venta.
 * Conservar paymentId como referencia consultable incluso tras reiniciar o perder la respuesta.
 * No devolver PAN completo, PIN, CVV, pistas, llaves ni respuestas crudas del SDK.
 * queryPayment y cancelPayment deben poder ejecutarse durante processPayment.
 */
public interface NiubizTerminalClient {

    PinpadAdapterResult processPayment(NiubizTerminalPaymentRequest payment);

    // CANCELLED solo si el proveedor confirma que no hubo cobro; si fue cobrado, APPROVED.
    // Cancelar una operacion pendiente no equivale a anular/devolver una venta aprobada.
    PinpadAdapterResult cancelPayment(String paymentId);

    // Solo consultar la operacion existente. Vacio significa resultado aun sin confirmar.
    Optional<PinpadAdapterResult> queryPayment(String paymentId);

    boolean isConnected();
}
