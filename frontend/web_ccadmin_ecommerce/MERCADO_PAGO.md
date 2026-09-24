# Mercado Pago en checkout

Seleccionar `TC001` (crédito) o `TD001` (débito) abre un modal con Card Payment Brick de Mercado Pago. El botón **Pagar con Mercado Pago** permite volver a abrirlo. Los demás medios conservan su flujo habitual.

## Instalación y configuración

1. Ejecutar `database/db_store_01_mysql/tables/table_mercado_pago_attempt.sql`.
2. Ejecutar `database/db_store_01_mysql/install_mercado_pago_ecommerce_20260917.sql`.
3. Actualizar la fila de `business_config` con `GroupCod = 'MercadoPagoEcommerce'` y `ConfigCod = 'MercadoPagoCheckout'`:

| Columna | Valor |
| --- | --- |
| `ConfigVal` | Public Key de la aplicación de Mercado Pago |
| `Str3Config` | Access Token privado de la misma aplicación/cuenta |
| `Str1Config` | `es-PE` |
| `Str2Config` | `PEN`; debe coincidir con la moneda del pedido y la cuenta |
| `Num1Config` | Máximo de cuotas; inicialmente `1`. Débito siempre usa una cuota |
| `Sta1Config` | `S` para habilitar; inicialmente `N` |
| `Sta2Config` | `S` para pruebas; `N` para producción |
| `Status` | `A` |

La definición de estas columnas está en `business_config_group`, con el mismo `GroupCod`. Los scripts pueden repetirse sin sobrescribir las credenciales ni la activación. Los valores se consultan desde base de datos; no se compilan en Angular.

4. Desplegar/reiniciar `backend/ws_wa_store_ccadmin` y desplegar el build de `frontend/web_ccadmin_ecommerce`. Usar HTTPS en el ecommerce; localhost permite desarrollo.
5. Utilizar las credenciales y los compradores/tarjetas de prueba que correspondan a la aplicación de Mercado Pago. El correo del pagador se ingresa en el Brick para permitir el comprador de prueba. No usar una tarjeta real durante las pruebas.

Los scripts se ejecutaron inicialmente en la base local `localhost:3306/db_store_01` con credenciales vacías. Si se actualiza una instalación existente, volver a ejecutar `tables/table_mercado_pago_attempt.sql` antes del backend para incorporar `ProviderStatusDetail` sin modificar los pagos existentes.

## Comportamiento

- Angular carga `https://sdk.mercadopago.com/js/v2`. Los datos de tarjeta se tokenizan en los campos seguros de Mercado Pago. El servidor recibe el token, nunca PAN ni CVV.
- La API autenticada `/api/v1/delivery/sale/mercadoPago/{configuration,pay,status}` valida el acceso al pedido mediante `OrderToken`. La respuesta de configuración contiene únicamente Public Key, idioma, moneda, importe, cuotas, modo y estado del pago.
- El backend obtiene el importe de la venta y usa `/v1/payments`. Cada intento conserva un UUID como `X-Idempotency-Key` y `external_reference`, junto con una huella del formulario; no persiste el token de tarjeta.
- Solo `approved`, con referencia, moneda, monto, modo de prueba y tipo de tarjeta válidos, registra la transacción usando los servicios de pagos existentes. La venta queda pagada y pendiente de confirmación operativa por la tienda.
- Un resultado pendiente o incierto bloquea otro cobro y la anulación/expiración del pedido. **Consultar estado** verifica el resultado. Si se pierde la respuesta, **Reintentar el mismo pago** reutiliza token y UUID; después de diez minutos el servidor solo consulta el resultado.
- Una tarea de backend consulta cada minuto hasta 50 intentos pendientes y delega en el mismo servicio de registro. También recupera pagos cuando se cerró el navegador. Esta integración no requiere configurar una URL pública de webhook.
- Un rechazo definitivo permite un intento nuevo. Los demás medios no pueden registrar manualmente `TC001`/`TD001` para saltarse Mercado Pago.
- Si Mercado Pago no tiene un resultado para una solicitud incierta, el intento permanece bloqueado. Comprobarlo en la cuenta de Mercado Pago antes de una conciliación administrativa; no cambiarlo a fallido solo por tiempo transcurrido. No cambiar las credenciales de cuenta mientras existan intentos pendientes: su huella evita reenviar el intento a otra cuenta.
- Reembolsos y contracargos posteriores a un pago ya registrado requieren gestión separada; esta integración cubre el cobro del checkout y su conciliación inicial.

## Prueba funcional

Para simular aprobación, usar una tarjeta de prueba oficial y escribir **APRO** en el campo de nombre del titular del Brick, con el documento correspondiente al escenario publicado por Mercado Pago. Un nombre personal no equivale al escenario de aprobación. **OTHE** permite simular un rechazo general. El titular se tokeniza dentro del SDK; no aparece como campo independiente en el JSON enviado al backend. Ver [tarjetas y escenarios de prueba](https://www.mercadopago.com.pe/developers/es/docs/checkout-api-payments/additional-content/your-integrations/test/cards).

`Status: "200"` y `ErrorStatus: false` indican que nuestra API pudo atender la solicitud. El resultado del cobro está en `Data.State`: `C` aprobado y registrado, `F` rechazado/cancelado y `P` en verificación. `Data.ProviderStatus` y `Data.ProviderStatusDetail` conservan los códigos de Mercado Pago. Un `cc_rejected_other_reason` es un rechazo general; no identifica por sí solo una causa específica. Los intentos anteriores a la incorporación de `ProviderStatusDetail` pueden tener ese campo vacío.

Crear un pedido y probar `TC001` y `TD001`: apertura/cierre/reapertura del modal, aprobación, rechazo, respuesta pendiente, doble clic y recuperación tras recargar la página. Verificar que un pago aprobado produce una sola fila en `sale_payments`, que `TransactionId` corresponde a Mercado Pago y que no se guardan CVV ni número completo de tarjeta. Comprobar también un medio manual existente.

Referencias oficiales: [Card Payment Brick](https://www.mercadopago.com.pe/developers/es/docs/checkout-bricks/card-payment-brick/default-rendering), [envío del pago](https://www.mercadopago.com.pe/developers/es/docs/checkout-bricks/card-payment-brick/payment-submission), [contrato del SDK](https://github.com/mercadopago/sdk-js/blob/main/docs/bricks/card-payment.md).
