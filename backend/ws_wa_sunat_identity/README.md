# ws_wa_sunat_identity

Microservicio Spring Boot para consultar información pública de identidad tributaria en SUNAT.

## Requisitos

- Java 21
- Maven 3.9

El servicio escucha por defecto en el puerto `8093`.

## Endpoints

### Consulta de empresa por RUC

```http
GET /api/v1/sunatIdentity/findCompanyByRuc?Ruc=20123456789
```

Respuesta:

```json
{
  "Status": "200",
  "Message": "operation performed successfully",
  "Data": {
    "found": true,
    "message": "Consulta realizada correctamente.",
    "company": {
      "ruc": "20123456789",
      "legalName": "EMPRESA EJEMPLO S.A.C.",
      "taxpayerType": "SOCIEDAD ANONIMA CERRADA",
      "tradeName": "EMPRESA EJEMPLO",
      "registrationDate": "15/03/2018",
      "businessStartDate": "01/04/2018",
      "taxpayerStatus": "ACTIVO",
      "taxpayerCondition": "HABIDO",
      "fiscalAddress": "AV. EJEMPLO 123 LIMA - LIMA - MIRAFLORES",
      "receiptIssuanceSystem": "COMPUTARIZADO",
      "foreignTradeActivity": "SIN ACTIVIDAD",
      "accountingSystem": "COMPUTARIZADO",
      "economicActivities": [
        "Principal - 6201 - Programación informática"
      ],
      "authorizedPaymentReceipts": [
        "FACTURA",
        "BOLETA DE VENTA"
      ],
      "electronicIssuanceSystems": [
        "FACTURA PORTAL"
      ],
      "electronicIssuerSince": "01/04/2018",
      "electronicReceipts": [
        "FACTURA",
        "BOLETA"
      ],
      "pleMemberSince": null,
      "registries": [
        "NINGUNO"
      ],
      "queryDate": "04/08/2026 10:15"
    }
  },
  "ErrorStatus": false,
  "ErrorID": 0,
  "DataAdditional": []
}
```

### Consulta por documento de persona

```http
GET /api/v1/sunatIdentity/findPersonByDocument?DocumentType=01&DocumentNumber=12345678
```

Tipos documentales canónicos:

- `01`: DNI
- `04`: carnet de extranjería
- `07`: pasaporte
- `A`: cédula diplomática

Esta operación devuelve los contribuyentes/RUC asociados al documento en la consulta pública de SUNAT. No es una consulta de identidad civil a RENIEC.

## Configuración

```properties
identity.query.provider=sunat
sunat.identity.base-url=https://e-consultaruc.sunat.gob.pe
sunat.identity.connect-timeout=20s
sunat.identity.request-timeout=40s
sunat.identity.user-agent=Mozilla/5.0 ...
```

`IdentityQueryProvider` es el contrato reemplazable. La implementación activa es `SunatIdentityQueryProvider`; otra fuente puede incorporarse sin cambiar el controller ni `SunatIdentitySearchService`.

Cada consulta abre una sesión HTTP independiente para aislar cookies y tokens entre solicitudes concurrentes. Ante un token rechazado se renueva la sesión una sola vez.

## Ejecución

Desde este directorio, si Maven está instalado:

```shell
mvn spring-boot:run
```

Los endpoints no requieren autenticación, igual que el microservicio interno `ws_wa_sunat_ccadmin`. En producción deben exponerse solamente mediante la red interna o el gateway autorizado.
