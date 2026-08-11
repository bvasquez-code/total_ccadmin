# Web CcAdmin Ecommerce

Tienda virtual publica de CcAdmin. Es una aplicacion Angular 15 independiente de `web_ccadmin_lt`.

## Ejecutar

```bash
npm install
npm start
```

El backend local se configura en `src/environments/environment.ts`.

## Contratos publicos

La aplicacion no consume los controllers administrativos. Espera las mascaras publicas del paquete `delivery`:

- `GET /api/v1/delivery/store/resolveByIp`
- `POST /api/v1/delivery/store/resolveLocation`
- `POST /api/v1/delivery/productSearch/query`
- `GET /api/v1/delivery/productSearch/findAvailability`
- `POST /api/v1/delivery/clientAccount/login`
- `POST /api/v1/delivery/clientAccount/register`
- `GET /api/v1/delivery/presale/createCode`
- `POST /api/v1/delivery/presale/save`

El checkout construye en el frontend el mismo `PresaleRegisterDto` utilizado por la venta interna: `Headboard`, `DetailList`, `PresaleChannel` y `CreditNoteCod`. El contrato web lo extiende solamente con `Delivery`. El adaptador público valida nuevamente tienda, precio, unidad y stock, asigna el cliente autenticado y delega el mismo objeto en `PresaleCreateService`.

Estos contratos se implementan en el backend bajo `com.ccadmin.app.delivery`. La ubicacion por IP usa por defecto `ipwho.is` y puede reemplazarse mediante la propiedad `delivery.ip-geolocation.url`; cuando existe un proxy, este debe propagar `X-Forwarded-For` o `X-Real-IP`.

El registro crea o reutiliza el cliente por documento, genera la cuenta con BCrypt y la deja verificada para iniciar sesion inmediatamente.
