# Agente local de pinpad

Servicio Java 21 / Spring Boot que recibe solicitudes de la web en
`127.0.0.1:6669`, serializa el uso del terminal y conserva los resultados en
archivos locales. Todas las peticiones requieren `X-Agent-Token`.

## Estado de la integracion Culqi

El modo **demo** funciona con el simulador existente. El modo **culqi** tiene
selector, adaptador, validaciones y conciliacion preparados, pero **todavia no
se comunica con un equipo fisico**. Falta implementar `CulqiTerminalClient`
con el SDK/protocolo que Culqi entregue para el modelo contratado. El servicio
rechaza el arranque en modo Culqi si falta ese bean; nunca cae al simulador.

`CulqiTerminalClient` y `CulqiTerminalPaymentRequest` son contratos internos de
este proyecto, no clases oficiales de Culqi. No se han inventado endpoints,
tramas seriales, puertos USB ni credenciales del fabricante.

La documentacion publica consultada en https://docs.culqi.com/ y
https://apidocs.culqi.com/ describe pagos online (tokens, cargos, etc.). No es
el contrato de comunicacion de un pinpad fisico y sus llaves no bastan para
controlar el terminal desde Windows.

## Seleccion en application.yml

```yaml
pinpad:
  provider: demo # demo | culqi
```

Tambien se admite `PINPAD_PROVIDER`. El valor por defecto es `demo`. Un valor
desconocido impide iniciar el servicio. La propiedad antigua
`simulator-enabled` se respeta cuando no se declara `provider`: `true` equivale
a demo y `false` selecciona Culqi, sujeto a sus requisitos. Si se especifican
ambas, `provider` tiene prioridad.

`GET /health` conserva sus campos y agrega `provider`. `simulatorEnabled`
refleja el proveedor seleccionado. El token local no es una llave de Culqi.

## Lo que debes obtener de Culqi

1. Modelo exacto del terminal y confirmacion de que admite integracion con
   una caja en Windows. Comprar un POS no demuestra que tenga esa capacidad.
2. SDK/librerias y manual de integracion presencial para ese modelo, versiones
   soportadas, arquitectura de Windows y drivers necesarios (si corresponde).
3. Canal autorizado de conexion: USB/COM, red, servicio del fabricante u otro,
   con su autenticacion y configuracion oficial.
4. Identificadores de comercio y terminal; entorno y equipo de homologacion.
5. Contrato de venta, consulta por referencia, cancelacion, codigos de
   respuesta, monedas y medios de pago admitidos. Debe poder consultarse una
   venta usando la referencia enviada incluso tras perder su respuesta.
6. Procedimiento de habilitacion y homologacion para produccion.

## Trabajo pendiente cuando llegue el SDK

Implementar un bean Spring `CulqiTerminalClient` dentro de
`com.local.app.pinpad.adapter.culqi`, incorporar la dependencia oficial y
configurar los parametros de conexion que indique su manual. No duplicar
persistencia ni flujo de pagos: el adaptador delega en ese cliente y el
servicio comun registra, concilia y confirma lectura.

El cliente debe:

- Enviar `amountCents` como entero en centimos, o convertir exactamente si el
  SDK usa otra unidad. Conservar `paymentId` como referencia consultable e
  idempotente; si el protocolo no lo permite, sera necesario persistir la
  correlacion oficial antes de enviar el cobro.
- Tener tiempos de espera finitos en venta, consulta, cancelacion y salud;
  no reintentar automaticamente una venta. `timeoutSeconds` se entrega al
  cliente en la solicitud; el timeout del agente por si solo no interrumpe
  una DLL ni cancela una transaccion bancaria.
- Permitir consultar/cancelar mientras hay una venta en curso, segun el
  mecanismo que el SDK soporte. Cerrar sus recursos al detener Spring.
- Mapear solo resultados confirmados: `APPROVED` exige `transactionId`;
  `REJECTED` o `CANCELLED` exigen confirmacion del proveedor de que no hubo
  cobro. Una cancelacion que descubre un pago aprobado devuelve `APPROVED`.
  Cancelar esta operacion no implementa una devolucion de dinero.
- Usar `PROCESSING` para una operacion aun pendiente, y `UNKNOWN` o una
  excepcion para fallos de comunicacion o resultados inciertos. Una consulta
  sin resultado devuelve `Optional.empty()` y nunca inicia otra venta.
- Devolver exclusivamente informacion permitida: identificadores de la
  transaccion, autorizacion, marca, ultimos cuatro digitos y voucher
  sanitizado. No enviar PAN completo, PIN, CVV, pistas, llaves ni respuestas
  crudas del SDK a DTOs, archivos o logs.

## Configuracion para el despliegue real

Una vez instalado y homologado el cliente del terminal, ajustar el
`application.yml` externo al JAR, por ejemplo:

```yaml
server:
  address: 127.0.0.1
  port: 6669
pinpad:
  provider: culqi
  agent-token: ${PINPAD_AGENT_TOKEN}
  storage-path: C:/pinpad-agent-culqi
  terminal-id: ${PINPAD_TERMINAL_ID}
  merchant-id: ${PINPAD_MERCHANT_ID}
  timeout-seconds: 120
  culqi-supported-payment-methods:
    - CARD
  allowed-origins:
    - https://tu-aplicacion.example.com
```

Asignar un token propio y los identificadores reales; configurar la web para
enviar ese token. Registrar su origen exacto y validar desde el navegador
real el acceso de la web al agente local. Mantener carpetas distintas para
demo y Culqi para no mezclar resultados simulados con reales. Agregar YAPE,
PLIN, WALLET o QR solo despues de comprobar que el contrato y el cliente los
soportan. Las credenciales y opciones adicionales del terminal se agregaran
segun el SDK oficial, sin publicarlas en el repositorio.

Ejecutar el agente con una cuenta que pueda escribir en su carpeta local.
Conservar esa carpeta entre reinicios: forma parte de la idempotencia y de
la recuperacion de pagos. El adaptador no requiere cambios en los endpoints
existentes de la web:

- `POST /payment-pinpad`: enviar una sola referencia `paymentId` por intento.
- `GET /payment-pinpad/{paymentId}/status` y `/search`: recuperar o conciliar
  la misma operacion, incluso despues de reiniciar el agente.
- `POST /payment-pinpad/{paymentId}/cancel`: solicitar cancelacion y respetar
  el estado devuelto.
- `POST /payment-pinpad/{paymentId}/ack`: confirmar lectura de un resultado
  confirmado; se rechaza mientras haya un pago Culqi sin resolver.
- `GET /payment-pinpad/{paymentId}/voucher`: recuperar el voucher disponible.

Ante timeout o perdida de comunicacion, Culqi queda `UNKNOWN`. Se conserva
el bloqueo de nuevas ventas y se consulta la misma referencia; no se debe
crear otro `paymentId` para repetir el cobro. Si la consulta aun no confirma
el resultado, sigue pendiente. No hay liberacion automatica ni endpoint
para forzar un resultado. Resolver con el proveedor si su sistema tampoco
puede determinarlo. Un cambio de proveedor tampoco permite cancelar o
reutilizar un pago del proveedor anterior. Lo mismo aplica al cambiar los
identificadores de comercio o terminal mientras haya operaciones pendientes:
restaurar la configuracion original para conciliarlas.

Antes de activar produccion, homologar con equipo real: aprobacion, rechazo,
cancelacion confirmada, cancelacion tras aprobacion, desconexion durante
venta, timeout con aprobacion tardia, reinicio durante venta, duplicados por
`paymentId`, recuperacion de voucher y ACK. Las pruebas automaticas actuales
usan un cliente simulado en tests y no certifican hardware Culqi.

## Compilacion y pruebas

Con Java 21 y Maven:

```powershell
mvn test
mvn package
java -jar target/ws-wa-pinpad-1.0.0.jar
```

Spring Boot admite un `application.yml` externo en el directorio de arranque
o una ubicacion explicita mediante `--spring.config.additional-location`.
