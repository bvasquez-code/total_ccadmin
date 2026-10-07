# Agente local de pinpad

Servicio Java 21 / Spring Boot que recibe solicitudes de la web en
`127.0.0.1:8094`, serializa el uso del terminal y conserva los resultados en
archivos locales. La web inicia sesion en `/pinpad/login` y usa el token temporal en las
URLs completas de cobro, consulta y confirmacion. La API administrativa
conserva `X-Agent-Token`.

## Instalacion rapida

1. Ejecutar el JAR desde su carpeta de instalacion. Crea `pinpad-cajas.yml`
   automaticamente con puerto 8094 y proveedor demo.
2. Editar ese unico archivo: identidad de caja, tienda, codigo de caja y origen
   de la web. Los valores `${VARIABLE:valor}` ya tienen un valor predeterminado;
   se pueden reemplazar por el valor deseado sin crear variables de entorno.
3. Registrar identificador, tienda y caja en el `pinpad-cajas.yml` del backend.
   Para nube, ajustar `backend-authorization-url` a la URL HTTPS completa del backend.
   Si ambos servicios arrancan desde una misma carpeta, comparten ese archivo.

Las URLs locales y tiempos ya tienen valores por defecto. La configuracion
existente se conserva al reiniciar. Ver la seccion de instalacion por PC para
backend central y configuraciones de varias cajas.

## Estado de las integraciones presenciales

El modo **demo** funciona con el simulador existente. El modo **culqi** tiene
selector, adaptador, validaciones y conciliacion preparados, pero **todavia no
se comunica con un equipo fisico**. Falta implementar `CulqiTerminalClient`
con el SDK/protocolo que Culqi entregue para el modelo contratado. El servicio
rechaza el arranque en modo Culqi si falta ese bean; nunca cae al simulador.

El modo **niubiz** tambien tiene selector, adaptador, validaciones y
conciliacion preparados. Falta implementar `NiubizTerminalClient` con el contrato
presencial oficial del modelo y modalidad contratados, que puede ser una API
HTTPS y no requiere necesariamente un SDK nativo. **Todavia no hay comunicacion con
hardware Niubiz**. Seleccionarlo sin ese cliente impide arrancar; no utiliza
Culqi ni el simulador como alternativa.

Los dos proveedores usan `PinpadTerminalAdapter` para validar los resultados
y manejar fallos de transporte. `PinpadPaymentService` conserva un unico
flujo de idempotencia, persistencia, cancelacion, conciliacion y ACK.

`CulqiTerminalClient` y `CulqiTerminalPaymentRequest` son contratos internos de
este proyecto, no clases oficiales de Culqi. No se han inventado endpoints,
tramas seriales, puertos USB ni credenciales del fabricante.

La documentacion publica consultada en https://docs.culqi.com/ y
https://apidocs.culqi.com/ describe pagos online (tokens, cargos, etc.). No es
el contrato de comunicacion de un pinpad fisico y sus llaves no bastan para
controlar el terminal desde Windows.

## Seleccion en pinpad-cajas.yml

```yaml
pinpad:
  provider: demo # demo | culqi | niubiz
```

Tambien se admite `PINPAD_PROVIDER`. El valor por defecto es `demo`. Un valor
desconocido impide iniciar el servicio. La propiedad antigua
`simulator-enabled` se respeta cuando no se declara `provider`: `true` equivale
a demo y `false` selecciona Culqi, sujeto a sus requisitos. Si se especifican
ambas, `provider` tiene prioridad.

`GET /health` conserva sus campos y agrega `provider`. `simulatorEnabled`
refleja el proveedor seleccionado. El token local no es una llave del adquirente.

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
`pinpad-cajas.yml` externo al JAR, por ejemplo, conservando su perfil e identidad local:

```yaml
server:
  address: 127.0.0.1
  port: 8094
pinpad:
  provider: culqi
  storage-path: C:/pinpad-agent-culqi
  terminal-id: TERMINAL-REAL
  merchant-id: COMERCIO-REAL
  timeout-seconds: 120
  culqi-supported-payment-methods:
    - CARD
  allowed-origins:
    - https://tu-aplicacion.example.com
```

El token propio ya se genera en el primer arranque. Asignar los identificadores reales; usar ese token solamente para la API REST administrativa. Configurar
la URL de autorizacion del backend segun la seccion de instalacion indicada abajo. Registrar su origen exacto y validar desde el navegador
real el acceso de la web al agente local. Mantener carpetas distintas para
demo, Culqi y Niubiz para no mezclar sus resultados. Agregar YAPE,
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
  confirmado; se rechaza mientras haya un pago presencial sin resolver.
- `GET /payment-pinpad/{paymentId}/voucher`: recuperar el voucher disponible.

Ante timeout o perdida de comunicacion, Culqi y Niubiz quedan `UNKNOWN`. Se conserva
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
usan clientes simulados en tests y no certifican hardware Culqi ni Niubiz.

## Integracion Niubiz y requisitos para produccion

El portal publico consultado, https://desarrolladores.niubiz.com.pe/, muestra
soluciones digitales, ecommerce y APIs de pagos. No se encontro alli el
contrato para controlar un pinpad desde Windows. No asumir que las APIs de
ecommerce ni documentos de otros adquirentes permiten manejar el terminal.
La dificultad y el transporte dependen del modelo y del kit autorizado.

### Modalidad HTTP hacia un POS fisico

El texto aportado por el usuario describe una modalidad denominada "POS Cloud":
el sistema envia el monto y un identificador del terminal a una API en la nube,
y esa plataforma comunica la orden al POS fisico. Este esquema es compatible
con el agente local y con `NiubizTerminalClient`; no presupone USB, COM ni DLLs.
El texto es una descripcion del flujo, no una especificacion tecnica. Aun no
se ha verificado el contrato oficial de esa modalidad ni la compatibilidad de
un terminal concreto. No tomar como confirmados sus endpoints, autenticacion,
requisitos comerciales o equivalencia entre TID y numero de serie.

Si Niubiz habilita esa modalidad, el flujo previsto es:

```text
Web -> agente local Java -> API HTTPS del proveedor -> POS fisico
Web <- agente local Java <- consulta de la misma operacion <- resultado del POS
```

`pinpad.terminal-id` ya almacena el identificador del terminal. Usar exactamente
el identificador que indique Niubiz, junto con comercio y credenciales del
servicio; no asumir que basta un numero de serie. Faltan las URLs de sandbox y
produccion, autenticacion, JSON de venta con referencia estable, consulta del
resultado, cancelacion y codigos de respuesta para implementar el cliente HTTP.
El metodo de autenticacion tampoco debe deducirse del resumen: la API publica
de seguridad documentada en
https://desarrolladores.niubiz.com.pe/reference/get_v1-security usa GET con Basic,
mientras que el texto aportado menciona POST. No asumir que esa API publica sea
la que corresponda a POS Cloud.

Para un agente que escucha solo en `127.0.0.1`, priorizar la consulta saliente
del resultado si el contrato la soporta. Un webhook del proveedor necesita un
receptor alcanzable por el proveedor; localhost no lo es. Si solo se ofrecen
webhooks, sera necesario un receptor publico y la recuperacion del resultado
desde el agente. Confirmar ese mecanismo antes de implementar el transporte.

Pedir a Niubiz:

1. Modelo y modalidad de **POS fisico integrado a caja**, indicando si se
   requiere una API HTTPS/cloud o un conector local para Windows, con
   confirmacion de compatibilidad con la PC y el software del comercio.
2. Contrato de integracion presencial oficial: manual de API/OpenAPI, SDK o protocolo,
   ejemplos, drivers y requisitos de arquitectura x86/x64 si aplican.
3. Parametros oficiales de conexion y autenticacion; codigo de comercio e
   identificador del terminal; entorno y equipo para homologacion.
4. Contratos de venta, consulta por referencia tras timeout/reinicio,
   cancelacion de operaciones pendientes, codigos de resultado, vouchers,
   monedas y medios de pago admitidos.
5. Procedimiento de homologacion, activacion y soporte de la integracion.

Con el contrato recibido, implementar un bean `NiubizTerminalClient` en
`com.local.app.pinpad.adapter.niubiz`: mediante HTTP si esa es la modalidad
habilitada, o incorporando el SDK oficial si corresponde.
`NiubizTerminalPaymentRequest` es una solicitud **interna del proyecto**, no
una trama ni una clase oficial Niubiz. El cliente traduce esa solicitud al
kit contratado y devuelve `PinpadAdapterResult`, respetando las reglas de
centimos, referencias estables, timeout finito y datos permitidos descritas
arriba para Culqi. Venta, consulta, cancelacion y salud se delegan en ese
cliente; no agregar otro flujo de persistencia ni reintentar cobros.

Registrar el bean solo para `pinpad.provider=niubiz`, por ejemplo con
`@ConditionalOnProperty(prefix = "pinpad", name = "provider", havingValue = "niubiz")`.
Si el SDK exige otro proceso o una arquitectura distinta a la JVM, adaptar
el cliente a ese mecanismo de acuerdo con el manual. No se agregaron DLLs,
COM, URLs, credenciales ni protocolos supuestos.

Despues de implementar y homologar el cliente, configurar:

```yaml
pinpad:
  provider: niubiz
  agent-token: ${PINPAD_AGENT_TOKEN}
  storage-path: C:/pinpad-agent-niubiz
  terminal-id: ${PINPAD_TERMINAL_ID}
  merchant-id: ${PINPAD_MERCHANT_ID}
  timeout-seconds: 120
  niubiz-supported-payment-methods:
    - CARD
  allowed-origins:
    - https://tu-aplicacion.example.com
```

Tambien se puede seleccionar con `PINPAD_PROVIDER=niubiz`. Mantener la
escucha en `127.0.0.1`, usar token propio e identificadores reales y agregar
las opciones de conexion exactas que requiera el kit. La lista de medios de
pago es independiente de Culqi: habilitar QR/billeteras solo si el cliente y
el contrato lo soportan. No cambiar de proveedor o terminal para resolver
una operacion incierta: conservar su almacenamiento y configuracion hasta
conciliarla. `/cancel` no implementa una anulacion o devolucion de una venta
aprobada; esos comandos requieren su propio contrato del proveedor.

## Instalacion en cada PC y backend local o en la nube

Cada PC que cobra ejecuta su propio `ws_wa_pinpad`, ligado a su terminal.
El **navegador de esa PC** llama a `127.0.0.1:8094`; el backend puede estar
localmente o en un servidor. No se requiere una IP publica ni un tunel por
cada PC. La direccion loopback en `business_config` es comun para todas las
cajas: se interpreta en el navegador de cada maquina.

El catalogo del sistema se traduce por `PaymentMethodType`: `1002` y `1003`
se envian como `CARD`; `1006` se envia como `YAPE`. El codigo interno original
(por ejemplo, `TJ001`, `TC001` o `WD001`) se conserva en `internalPaymentCode`.
Los otros tipos se registran sin llamar al agente. El modo demo admite Yape;
en Culqi/Niubiz solo debe habilitarse en `*-supported-payment-methods` cuando
el conector y el terminal contratado lo soporten.

El flujo implementado es:

1. La web pide autorizacion a `POST /api/v1/TrxPayment/preparePinpad`.
   El backend valida identidad de caja, medio, importe y moneda, y autoriza
   una referencia concreta. No exige apertura de caja.
2. La web llama a la URL de login del agente, enviando esa autorizacion y la
   sesion web existente. El cajero no introduce otra contrasena.
3. El agente valida la sesion con el backend usando la URL completa
   `backend-authorization-url` y recibe la autorizacion para esa caja.
   Devuelve un token local aleatorio y temporal; no reutiliza el JWT web
   como token de las operaciones locales.
4. La web envia `Authorization: Bearer <token-local>` a las URLs configuradas
   de registro y consulta. Ambas son POST con cuerpo `{}`: los datos del
   cobro ya quedaron asociados al token durante el login.
5. El agente delega en `PinpadPaymentService` y firma su comprobante. La web
   lo entrega en `PinpadResult` a `POST /api/v1/TrxPayment/save`.
   El backend verifica identidad, firma, sesion, usuario, medio, moneda,
   tipo de cambio, referencia e importe antes de guardar.
6. Despues del commit, el backend autoriza ACK. La web realiza otro login
   automatico con esa autorizacion y usa el token obtenido en la URL de ACK.
   Un token de cobro no puede ejecutar ACK; uno de ACK no puede cobrar.

El token dura como maximo 15 minutos y se limita al pago y sesion autorizados.
Si vence, la web obtiene una nueva autorizacion al reintentar con la misma
referencia; esto recupera el pago existente. Los comprobantes aprobados
siguen verificandose despues del vencimiento del token.

### URLs completas en business_config

El grupo `PinpadServiceUrl` contiene cuatro URLs completas e independientes:

| ConfigCod | ConfigVal |
| --- | --- |
| `PinpadLoginUrl` | `http://127.0.0.1:8094/pinpad/login` |
| `PinpadRegisterPaymentUrl` | `http://127.0.0.1:8094/pinpad/payment/register` |
| `PinpadPaymentStatusUrl` | `http://127.0.0.1:8094/pinpad/payment/status` |
| `PinpadPaymentAckUrl` | `http://127.0.0.1:8094/pinpad/payment/ack` |

**El codigo utiliza cada URL tal como esta guardada. No agrega rutas,
identificadores ni sufijos.** El pago se identifica mediante el token local.
El login recibe `{authorization, applicationToken}` automaticamente desde
la web; las otras tres URLs reciben `{}` y el token en el header Authorization.

La fila de registro usa `Num1Config` como espera en segundos (130 por defecto,
rango 0..180) y `Num2Config` como intervalo en ms (1000, rango 100..5000).
Las llamadas locales tienen timeout de 10 segundos; login, 15 segundos.
Si faltan las filas o sus valores estan vacios, se usan las cuatro URLs
indicadas y sus tiempos por defecto. Una fila inactiva se sigue rechazando.
`Str3Config` no almacena claves ni contrasenas.

Ejecutar en este orden antes del despliegue:

1. `database/db_store_01_mysql/tables/table_trx_payments.sql`.
2. `database/db_store_01_mysql/tables/table_trx_payments_document.sql`, si falta.
3. `database/db_store_01_mysql/install_pinpad_service_urls_20261007.sql`.

El ultimo script agrega login y migra las antiguas URLs `/browser-pinpad`
de puertos 6670/8094, y las predeterminadas originales de 6669, a las tres
rutas explicitas de arriba. Conserva URLs personalizadas y tiempos. Si habia
URLs propias, configurar cada ruta completa manualmente. El endpoint
`/browser-pinpad` se retiro. No usar 6669: el
[estandar Fetch](https://fetch.spec.whatwg.org/#port-blocking) lo bloquea.
Si se cambia el puerto con `PINPAD_PORT`, actualizar las cuatro URLs.

### Configuracion por instalacion

El primer arranque del JAR crea `pinpad-cajas.yml` en su directorio de trabajo.
Si ya existe, lo conserva. Tambien admite `config/pinpad-cajas.yml`, con prioridad.
El backend carga automaticamente el mismo nombre de archivo, sin parametros
extra de arranque. El perfil inicial es CAJA01, tienda T001 y caja CAJA0001.

Si el usuario no utiliza sesiones de caja, el backend selecciona el unico agente
configurado para su tienda autenticada. `CashSessionID` permanece nulo: no se
crea ni se abre una caja contable. Con varias cajas en la misma tienda, la
seleccion actual requiere una sesion que identifique la caja; sin ese dato se
rechaza la ambiguedad antes de cobrar. Los clientes con otra tienda deben
ajustar `store-cod` en `pinpad-cajas.yml`.

En cada PC ajustar solo ese archivo: `agent-id`, proveedor, terminal/comercio,
`allowed-origins` y la URL del backend. Para backend local ya existe este valor:

```yaml
pinpad:
  agent-id: CAJA01
  backend-authorization-url: http://127.0.0.1:8090/api/v1/TrxPayment/authorizePinpad
```

Para nube reemplazar **la URL completa**, por ejemplo:
`https://api.tu-cliente.example/api/v1/TrxPayment/authorizePinpad`.
Debe usar HTTPS salvo que el backend este en localhost. No se completa ninguna
ruta desde el codigo. El agente necesita acceso al backend durante el login.

En `pinpad-cajas.yml` del backend registrar los codigos reales de cada terminal:

```yaml
pinpad:
  browser:
    agents:
      CAJA01:
        store-cod: "T001"
        register-cod: "CAJA0001"
      CAJA02:
        store-cod: "T001"
        register-cod: "CAJA0002"
```

No se configuran `signing-key`, `browser-signing-key` ni contrasenas por PC.
Los archivos antiguos pueden conservar esos campos: ya no se usan.
El token administrativo existente se genera automaticamente y no interviene
al cobrar desde la web. Cambiar proveedor requiere el conector fisico oficial.

La identidad tecnica del agente se genera internamente en
`<storage-path>/identity-rsa.json`. La primera sesion web valida para una caja
registra automaticamente su clave publica en el backend. Esa primera vinculacion
confia en la sesion autorizada y la PC donde se esta instalando; no es una
certificacion del hardware del adquirente. Las siguientes sesiones deben
presentar la misma identidad. El backend conserva sus datos en
`./data/pinpad-security` (configurable con `pinpad.security.storage-path`).
Conservar ambas carpetas al actualizar/reinstalar. No se copian claves privadas
entre PCs ni se incluyen en respuestas o archivos del repositorio.
Para varios nodos del mismo backend, compartir ese directorio persistente.

La web debe figurar en `allowed-origins`. En produccion usar HTTPS y permitir
el acceso a la red local cuando el navegador lo solicite; ver
[documentacion de Chrome](https://developer.chrome.com/blog/local-network-access).

### Reintentos y recuperacion

Crear `PinpadPaymentId` una sola vez antes del primer envio y conservarlo
hasta resolver el intento. El formulario ya lo hace, evita dobles clics y
permite consultar manualmente cuando la espera termina. Si el POST local
pierde su respuesta, consulta STATUS con esa referencia, sin repetir un
cobro con otro ID. El backend usa el codigo ISO de `CurrencyCod`, o
`CurrencyAbbr` para catalogos con identificadores internos.

Si se pierde la respuesta central o falla la BD despues de aprobar, repetir
`save` con el comprobante y la referencia originales. Un pago ya guardado
se devuelve sin volver a cobrar y obtiene un ACK nuevo. Si solo falla el
ACK, el pago central sigue siendo exitoso. El resultado conserva
`ResponseWsDto`, con el `TrxPaymentEntity` en `Data` y estado `OK`.

Sin aprobacion confirmada, `ErrorStatus=true`; `Data` incluye referencia,
`PaymentStatus` y `CanStartNewPayment`. Solo REJECTED y CANCELLED permiten
iniciar otro intento. PROCESSING, UNKNOWN, TIMEOUT, ERROR o una aprobacion
pendiente de guardar requieren la misma referencia y conciliacion.

Mantener abierto el formulario durante un cobro incierto: despues de cerrar
se necesita recuperar la solicitud y referencia originales. La idempotencia
se garantiza por referencia, no por importe; otro ID puede iniciar otro cobro.
Mantener las claves, identidad y almacenamiento del agente hasta conciliar
los pagos pendientes. Cada ingreso POS de `saveAll` necesita su comprobante
firmado y usa el mismo comando de dominio; los cobros fisicos no tienen
rollback por deshacer una transaccion SQL.

Efectivo, otras plataformas web y extornos contables mantienen su flujo.
Este cambio no implementa devoluciones de tarjeta. La integracion se puede
probar con `provider=demo`; Culqi/Niubiz siguen requiriendo el conector real
presencial descrito arriba. Actualizar backend, frontend y agente juntos.

## Compilacion y pruebas

Con Java 21 y Maven:

```powershell
mvn test
mvn package
java -jar target/ws-wa-pinpad-1.0.0.jar
```

Spring Boot admite un `application.yml` externo en el directorio de arranque
o una ubicacion explicita mediante `--spring.config.additional-location`.
