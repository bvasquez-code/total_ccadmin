# ws_wa_sunat_ccadmin

Servicio backend para facturacion electronica SUNAT Peru dentro del ecosistema `ccadmin`.

## Analisis del proyecto base

Referencia principal revisada: `backend/ws_wa_store_ccadmin`.

Patron encontrado:

- Spring Boot 3.2.0, Java 21 y Maven.
- Paquete base `com.ccadmin.app`.
- Organizacion por modulo funcional, no por capas globales puras.
- Controllers con `@RestController`, `@RequestMapping("api/v1/<recurso>")`, acciones como `findById`, `findAll`, `findDataForm`, `save`, `enable`, `disable`.
- Respuesta estandar `ResponseEntity<ResponseWsDto>`; los controllers capturan excepciones y responden `BAD_REQUEST`.
- Persistencia con Spring Data JPA y consultas nativas donde se necesita busqueda.
- Entidades con campos publicos, sufijo `Entity`, auditoria heredada de `AuditTableEntity`.
- Servicios separados por responsabilidad: `*CreateService`, `*SearchService` y servicios especializados.
- Base de datos MySQL con scripts `tables/table_<tabla>.sql`, procedimientos temporales `p_manage_<tabla>` e instrucciones rerunnable.

Nombre propuesto y creado:

- `backend/ws_wa_sunat_ccadmin`

El nombre conserva el prefijo `ws_wa_` y el sufijo `ccadmin`, y separa el contexto SUNAT de la logica comercial de ventas.

## Alcance tributario

Este servicio no genera ventas, notas, clientes, series comerciales ni correlativos comerciales. La responsabilidad es electronica y tributaria:

- registrar referencia al documento origen;
- generar XML UBL 2.1;
- firmar XML;
- comprimir ZIP;
- enviar a SUNAT;
- consultar tickets;
- procesar CDR;
- guardar archivos, respuestas, estados, errores e intentos.

La fase 1 deja implementado el registro base y la persistencia. Las operaciones tecnicas posteriores no simulan exito; los endpoints existen y devuelven que su fase esta pendiente.

## Integracion con datos comerciales

La integracion oficial es por API/payload. Este servicio es aislado del dominio comercial:

- `SourceModule`
- `SourceDocumentCod`
- `SourceDocumentType`
- `SunatDocumentType`
- `Series`
- `Correlative`
- emisor, cliente, detalle, impuestos, totales y documentos relacionados

El sistema principal debe enviar la informacion comercial ya calculada. Este backend no consulta ni conoce tablas como `sale_*`, `credit_note_*`, `client`, `product` u otras tablas comerciales.

## Tablas propuestas

Todas usan prefijo `sunat_` y se ubican en `database/db_store_01_mysql/tables`.

- `sunat_config`: configuracion activa SUNAT, credenciales SOL en texto plano para esta primera version, certificado, ambiente, endpoints, almacenamiento, reintentos y scheduler.
- `sunat_document`: documento electronico tributario, relacion con documento origen, tipo SUNAT, serie, correlativo, estado, ticket, respuesta SUNAT, errores y fechas finales.
- `sunat_document_payload`: payload JSON recibido por API y XML UBL 2.1 generado sin firma.
- `sunat_document_file`: referencias a XML, XML firmado, ZIP enviado, ZIP CDR, XML CDR, respuesta tecnica o error.
- `sunat_document_attempt`: trazabilidad por intento de generacion, firma, ZIP, envio, consulta ticket, reintento y scheduler.

## Estados internos

Campo: `ElectronicStatus`.

- `PEN`: pendiente.
- `GEN`: XML generado.
- `FIR`: XML firmado.
- `ZIP`: comprimido.
- `ENV`: enviado.
- `TCK`: pendiente de consulta de ticket.
- `ACE`: aceptado.
- `OBS`: aceptado con observaciones.
- `REJ`: rechazado.
- `ERR`: error.
- `RET`: pendiente de reintento.
- `ANU`: anulado o dado de baja.

## Endpoints fase 1

Configuracion:

- `GET api/v1/sunat/config/findById`
- `GET api/v1/sunat/config/findAll`
- `GET api/v1/sunat/config/findActive`
- `GET api/v1/sunat/config/findDataForm`
- `POST api/v1/sunat/config/save`
- `POST api/v1/sunat/config/activate`
- `POST api/v1/sunat/config/enable`
- `POST api/v1/sunat/config/disable`

Documento electronico:

- `POST api/v1/sunat/document/register`
- `POST api/v1/sunat/document/generateXml`
- `POST api/v1/sunat/document/generateXmlById`
- `GET api/v1/sunat/document/findById`
- `GET api/v1/sunat/document/findAll`
- `GET api/v1/sunat/document/findStatus`
- `GET api/v1/sunat/document/findFiles`
- `GET api/v1/sunat/document/findAttempts`
- `GET api/v1/sunat/document/findResponse`
- `POST api/v1/sunat/document/signXml`
- `POST api/v1/sunat/document/generateZip`
- `POST api/v1/sunat/document/send`
- `POST api/v1/sunat/document/consultTicket`
- `POST api/v1/sunat/document/retry`

## Fases pendientes

Fase 2 implementada:

- Generacion XML UBL 2.1 para factura, boleta, nota de credito y nota de debito.
- Validaciones tributarias de cliente, moneda, impuestos, operaciones gravadas/exoneradas/inafectas/gratuitas y documentos relacionados.
- Persistencia del payload JSON recibido y XML sin firmar en `sunat_document_payload`.
- Estado `GEN` e intento `GENERATE_XML` cuando la generacion termina correctamente.
- Rechazo funcional si faltan datos obligatorios.

Ejemplo minimo para `POST api/v1/sunat/document/generateXml`:

```json
{
  "SourceModule": "SALE",
  "SourceDocumentCod": "SALE000000000001",
  "SourceDocumentType": "SALE",
  "SunatDocumentType": "01",
  "Series": "F001",
  "Correlative": 1,
  "IssueDate": "2026-06-30T00:00:00.000-05:00",
  "IssueTime": "10:30:00",
  "CurrencyCod": "PEN",
  "OperationTypeCode": "0101",
  "Supplier": {
    "DocumentType": "6",
    "DocumentNumber": "20123456789",
    "LegalName": "EMPRESA DEMO SAC",
    "TradeName": "EMPRESA DEMO",
    "Address": "AV. DEMO 123",
    "UbigeoCod": "150101",
    "Department": "LIMA",
    "Province": "LIMA",
    "District": "LIMA",
    "CountryCode": "PE"
  },
  "Customer": {
    "DocumentType": "6",
    "DocumentNumber": "20987654321",
    "LegalName": "CLIENTE DEMO SAC",
    "Address": "CALLE CLIENTE 456",
    "CountryCode": "PE"
  },
  "Totals": {
    "TaxableAmount": 100.00,
    "TaxAmount": 18.00,
    "LineExtensionAmount": 100.00,
    "TaxInclusiveAmount": 118.00,
    "PayableAmount": 118.00
  },
  "Lines": [
    {
      "ItemNumber": 1,
      "ProductCode": "P001",
      "Description": "PRODUCTO DEMO",
      "UnitCode": "NIU",
      "Quantity": 1,
      "UnitPrice": 100.00,
      "PriceAmount": 118.00,
      "PriceTypeCode": "01",
      "LineExtensionAmount": 100.00,
      "TaxableAmount": 100.00,
      "TaxAmount": 18.00,
      "TaxPercent": 18,
      "TaxCategoryCode": "S"
    }
  ]
}
```

Fase 3 implementada:

- Firma digital con certificado `.p12`, `.pfx` o `.jks`.
- ZIP SUNAT.
- Escritura fisica de archivos por RUC, fecha, tipo, serie y correlativo.
- Hash SHA-256 y registro en `sunat_document_file`.
- `signXml` guarda el XML sin firmar como archivo, firma el XML desde `sunat_document_payload`, guarda el XML firmado y cambia el estado a `FIR`.
- `generateZip` comprime el ultimo XML firmado, guarda el ZIP y cambia el estado a `ZIP`.
- La firma usa `javax.xml.crypto.dsig` y la clave privada del certificado configurado en `sunat_config`.

Fase 4:

- Cliente SUNAT BETA/PRODUCCION.
- Envio de comprobantes individuales, resumen diario y comunicacion de baja.
- Consulta de tickets.
- Procesamiento de CDR y actualizacion final de estados.

Fase 5:

- Reintentos controlados.
- Scheduler funcional configurable desde `sunat_config`.
- Busqueda avanzada y descarga/visualizacion de archivos si el frontend lo requiere.

## Dependencias adicionales previstas

Fases 1 a 3 no agregan dependencias fuera del stack base de `ws_wa_store_ccadmin`.

Para fases siguientes se evaluaran:

- XML UBL: APIs XML estándar de Java o JAXB si se necesita mapeo fuerte.
- Firma XML: ya se usa XML Digital Signature de Java (`javax.xml.crypto.dsig`). Apache Santuario solo deberia agregarse si las pruebas SUNAT requieren compatibilidad adicional.
- SOAP/HTTP SUNAT: cliente HTTP estándar/Spring antes de introducir librerias SOAP extra.

No se deben hardcodear RUC, usuario SOL, clave SOL, certificado, clave de certificado ni endpoints.
