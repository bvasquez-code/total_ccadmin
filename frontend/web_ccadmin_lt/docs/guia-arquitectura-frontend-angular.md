# Guia de arquitectura y convenciones - Frontend Angular

## 1. Resumen general

Proyecto Angular administrativo llamado `web-ccadmin`, orientado a modulos empresariales: ventas, productos, compras, caja, transferencias, usuarios, sistema, pagos y configuracion.

La arquitectura es monolitica dentro de un unico `AppModule`. No se identifico lazy loading ni modulos Angular separados por funcionalidad. La separacion principal se hace por carpetas dentro de `src/app/enterprise`.

El estilo general es CRUD administrativo con paginas `list*`, `create*`, `view*`, servicios por dominio, modelos `Entity`/`Dto`, respuestas estandar `ResponseWsDto` y tablas reutilizables mediante `app-table`.

## 2. Versiones y dependencias

- Angular: `15.2.0`
- Angular CLI: `15.2.0`
- TypeScript: `~4.9.4`
- RxJS: `~7.8.0`
- Zone.js: `~0.12.0`
- Node runtime: No identificado en el proyecto analizado. Solo existe `@types/node ^18.19.130`.
- UI:
  - Bootstrap `4.6.0` via CDN en `index.html`
  - FontAwesome `5.15.3` via CDN
  - Ace Admin template desde `assets/dist/css/ace.min.css` y `assets/dist/js/ace.min.js`
  - Bootstrap Table via CDN
- Librerias relevantes:
  - `ngx-toastr`
  - `sweetalert2`
  - `qrcode`
  - `xlsx`
  - `@ng-bootstrap/ng-bootstrap`, aunque no se observa como patron dominante

## 3. Estructura de carpetas

Estructura principal:

```txt
src/app/
  app.module.ts
  app-routing.module.ts
  config/
  interceptors/
  enterprise/
    compartido/
    shared/
    main/
    login/
    sale/
    product/
    trxpayment/
    system/
    businessconfiggroup/
    cash/
    transfer/
    pucharse/
    client/
    supplier/
    user/
    menu/
    store/
```

Patron comun por modulo funcional:

```txt
enterprise/<modulo>/
  pages/
    list<entidad>/
    create<entidad>/
    view<entidad>/
  service/
  model/
    entity/
    dto/
    constants/
    pipes/
```

No todos los modulos tienen todas las carpetas. Los modelos compartidos se ubican en:

```txt
enterprise/shared/model/entity/
enterprise/shared/model/dto/
enterprise/shared/interface/
enterprise/shared/component/
enterprise/shared/service/
enterprise/shared/helper/
enterprise/shared/pipe/
```

## 4. Convenciones de nombres

- Carpetas: mayormente minusculas, sin guiones: `createpaymentmethod`, `listcurrency`, `businessconfiggroup`.
- Componentes:
  - Clase PascalCase con sufijo `Component`.
  - Ejemplo: `CreatepaymentmethodComponent`, `ListpaymentmethodComponent`.
  - Archivo Angular estandar: `.component.ts`, `.component.html`, ocasional `.component.css`.
- Servicios:
  - Clase PascalCase con sufijo `Service`.
  - Archivos mezclan estilos: `PaymentMethodService.ts`, `sale.service.ts`, `product.service.ts`.
- Entidades:
  - Sufijo `Entity`.
  - Ejemplo: `PaymentMethodEntity`, `TrxPaymentEntity`, `BusinessConfigEntity`.
- DTOs:
  - Sufijo `Dto`.
  - Ejemplo: `ResponseWsDto`, `SaleConfirmDto`, `CreditNoteReturnPaymentRegisterDto`.
- Interfaces:
  - Algunas usan prefijo `I`: `IRegisterForm`, `ICrudService`.
  - Otras son descriptivas sin prefijo funcional adicional: `ActionTableService`, `ActionModalConfirmService`.
- Metodos:
  - Mezcla de PascalCase y camelCase.
  - Ejemplos PascalCase: `FindDataForm`, `Save`, `GetParamUrl`, `LoadingForm`.
  - Ejemplos camelCase: `findAll`, `findById`, `save`, `enable`, `disable`.
- Variables:
  - Mezcla de PascalCase para parametros/codigos de negocio (`PaymentMethodCod`, `GroupCod`) y camelCase para estado local (`paymentMethodList`, `txtDocumentVisible`).

## 5. Arquitectura de modulos y rutas

Las rutas se definen centralizadamente en `app-routing.module.ts` con `RouterModule.forRoot(routes)`.

No se identifico lazy loading. Todas las paginas se importan directamente en `AppRoutingModule` y se declaran directamente en `AppModule`.

Convencion de rutas:

```txt
enterprise/<modulo>/pages/<pagina>
```

Ejemplos:

```txt
enterprise/system/pages/listpaymentmethod
enterprise/system/pages/createpaymentmethod
enterprise/sale/pages/createsale
enterprise/trxpayment/pages/listtrxpayment
```

Algunas rutas tienen mayusculas historicas, por ejemplo:

```txt
enterprise/product/pages/listProduct
enterprise/product/pages/createProduct
```

No se identificaron guards Angular (`CanActivate`) aplicados a rutas. El acceso se controla indirectamente con sesion/interceptores y con el menu por permisos.

## 6. Componentes

Los componentes suelen tener:

- `.ts` con estado, carga de datos y llamadas a servicios.
- `.html` con formularios Bootstrap/Ace.
- `.css` solo en algunos componentes.

Patrones frecuentes:

- `ngOnInit()` llama a `FindDataForm`, `findAll` o carga inicial.
- `@ViewChild` para inputs DOM (`txtSearch`, `txtAmountPaid`, `cboCurrencyCod`).
- `[(ngModel)]` para formularios simples.
- `@Input` / `@Output` para componentes reutilizables o modales, por ejemplo `app-createtrxpayment` y `app-appfile`.
- `EventEmitter` para emitir resultados de formularios embebidos.
- Navegacion con `Router.navigate(...)` o links `href`.

Metodos tipicos:

```ts
GetParamUrl(...)
FindDataForm(...)
Save()
validate(...)
filter(...)
findAll(...)
loadingTable(...)
getDataRow(...)
actionModal(...)
```

## 7. Servicios

Los servicios se ubican por modulo en `enterprise/<modulo>/service`.

Usan `ApiService` como wrapper HTTP. El patron moderno del proyecto es:

```ts
const rpt: ResponseWsDto = await this.apiService.ExecuteGetService(url, params);
const rpt: ResponseWsDto = await this.apiService.ExecutePostService(url, body);
```

Convencion de endpoints:

```txt
${AppSetting.API}/api/v1/<recurso>/<accion>
```

Ejemplo:

```ts
/api/v1/paymentMethod/findDataForm
/api/v1/paymentMethod/save
/api/v1/paymentMethod/enable
/api/v1/paymentMethod/disable
```

Metodos comunes de servicio:

```ts
findById(...)
findAll(Query, Page, ...)
findActives()
findDataForm(...)
save(entity)
saveAll(list)
enable(entity)
disable(entity)
```

## 8. Interfaces, modelos y DTOs

El frontend replica fuertemente DTOs y entidades del backend.

Tipos comunes:

- `Entity`: representa entidades de negocio.
- `Dto`: representa request/response compuesto o estructuras de pantalla.
- `ResponseWsDto`: respuesta estandar del backend.
- `ResponsePageSearch<T>`: respuesta paginada para tablas.
- `DataTablaGeneticDto<T>`: configuracion de tabla generica.

`ResponseWsDto` contiene:

```ts
Status
Message
Data
ErrorStatus
ErrorID
DataAdditional
```

`DataAdditional` se usa para catalogos, metadata y datos auxiliares de formularios, por ejemplo listas de monedas, medios de pago o configuracion dinamica.

## 9. Formularios y validaciones

El proyecto usa principalmente formularios template-driven y acceso directo a DOM con `ViewChild`.

Se observa:

- `FormsModule`
- `[(ngModel)]`
- `#txtCampo`
- `ElementRef<HTMLInputElement>`
- Validacion manual antes de guardar

No se observa uso dominante de `FormGroup`, `FormControl` o `Validators`.

La validacion comun esta en `ValidationHelper`:

```ts
validateIsNotEmpty(...)
validLengthString(...)
validNumber(...)
isValidString(...)
validateInList(...)
```

Los errores se muestran con `ToastrService.error(...)`.

## 10. UI y estilos

El estilo visual se basa en:

- Bootstrap 4
- Ace Admin
- FontAwesome
- Clases utilitarias del template Ace: `card`, `dcard`, `acard`, `bgc-*`, `brc-*`, `text-*`.
- Tablas administrativas con `app-table`.
- Modales Bootstrap con `data-toggle="modal"` y `data-target`.
- Confirmaciones con `app-modalconfirm` o `AlertService` basado en SweetAlert2.
- Notificaciones con `ngx-toastr`.

Estructura HTML tipica:

```html
<div role="main" class="main-content">
  <div class="page-content container container-plus">
    <div class="card acard mt-2 mt-lg-3">
      <div class="card-header">...</div>
      <div class="card-body">...</div>
    </div>
  </div>
</div>
```

## 11. Comunicacion con backend

La URL base sale de:

```ts
AppSetting.API = environment.settings.backend
```

El entorno local apunta a:

```ts
http://localhost:8090
```

`ApiService` agrega headers incluyendo:

```ts
Authorization: sessionStorage.getItem('Token')
Content-Type: application/json
```

Los metodos `ExecuteGetService`, `ExecutePostService` y `ExecuteDeleteService` devuelven `ResponseWsDto`.

Manejo tipico:

```ts
const rpt = await service.method(...);

if (!rpt.ErrorStatus) {
  // usar rpt.Data o rpt.DataAdditional
} else {
  this.toastrService.error(rpt.Message);
}
```

El spinner global se maneja con `SpinnerInterceptor`.

## 12. Seguridad, sesion y permisos

No se identificaron route guards.

La seguridad se maneja con:

- Token en `sessionStorage`.
- Header `Authorization` generado en `ApiService`.
- `BasicAuthHtppInterceptorService` que ante HTTP `401` limpia sesion y redirige a `/login`.
- `DataSesionService` que lee:
  - `Token`
  - `UserCod`
  - `PersonCod`
  - `Email`
  - `SessionID`
  - `StoreCod`
  - `Names`
  - `AppMenuPermissions`

Los permisos se aplican principalmente al menu:

```ts
PermissionExists(MenuCod)
```

El menu se construye manualmente en `menusidebar.component.ts`, con codigos como `SI000001`, `VT000003`, `PR000001`.

## 13. Reglas para nuevos desarrollos Frontend Angular

- Crear la pantalla dentro de `src/app/enterprise/<modulo>/pages/<pantalla>/`.
- Crear el servicio dentro de `src/app/enterprise/<modulo>/service/`.
- Crear entidades y DTOs en `model/entity` y `model/dto`; si se reutilizan entre modulos, colocarlos en `enterprise/shared/model`.
- Declarar el componente en `app.module.ts`.
- Registrar la ruta en `app-routing.module.ts`.
- Si debe aparecer en menu, agregar la opcion en `menusidebar.component.ts` con `url`, `url_position`, `url_shade`, `IsVisible` y permiso.
- Usar `AppSetting.API` y `ApiService`; no llamar `HttpClient` directamente desde componentes.
- Respetar `ResponseWsDto`: validar `ErrorStatus` antes de usar `Data`.
- Para listados paginados, usar `ResponsePageSearch<T>`, `DataTablaGeneticDto<T>` y `<app-table>`.
- Para formularios CRUD, seguir el patron `FindDataForm`, `Save`, `validate`, `GetParamUrl`.
- Para validaciones, usar `ValidationHelper` y mostrar errores con `ToastrService`.
- Para activar/inactivar, preferir `enable` / `disable`; no asumir eliminacion fisica.
- Para confirmaciones simples, usar `app-modalconfirm`; para confirmaciones ricas, usar `AlertService.waringHtml`.
- Mantener formularios con `ngModel` o `ViewChild` segun el patron cercano; no introducir reactive forms salvo que se refactorice una pantalla completa.
- No inventar modulos lazy-loaded: el proyecto actual centraliza rutas y declaraciones.
- Reutilizar entidades compartidas existentes antes de crear duplicados.

## 14. Ejemplo de estructura recomendada para una nueva pantalla o modulo

```txt
src/app/enterprise/<modulo>/
  pages/
    list<entidad>/
      list<entidad>.component.ts
      list<entidad>.component.html
    create<entidad>/
      create<entidad>.component.ts
      create<entidad>.component.html
  service/
    <Entidad>Service.ts
  model/
    entity/
      <Entidad>Entity.ts
    dto/
      <Entidad>RegisterDto.ts
```

Servicio recomendado:

```ts
@Injectable({ providedIn: 'root' })
export class EntidadService {
  constructor(private apiService: ApiService) {}

  async findAll(Query: string, Page: number): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/entidad/findAll`;
    return await this.apiService.ExecuteGetService(url, { Query, Page });
  }

  async findDataForm(Cod: string = ""): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/entidad/findDataForm`;
    return await this.apiService.ExecuteGetService(url, { Cod });
  }

  async save(entity: EntidadEntity): Promise<ResponseWsDto> {
    const url = `${AppSetting.API}/api/v1/entidad/save`;
    return await this.apiService.ExecutePostService(url, entity);
  }

  async enable(entity: EntidadEntity): Promise<ResponseWsDto> { ... }
  async disable(entity: EntidadEntity): Promise<ResponseWsDto> { ... }
}
```

Pantalla de lista recomendada:

- `ngOnInit()` llama `findAll(1, "")`.
- `filter(Page)` lee `txtSearch`.
- `loadingTable(...)` arma `DataTablaGeneticDto`.
- Opciones de tabla: editar por URL, activar/inactivar por modal.

Pantalla de creacion recomendada:

- Leer query params en constructor o `GetParamUrl`.
- Cargar `findDataForm`.
- Bindear campos con `[(ngModel)]` o `ViewChild`.
- Validar con `ValidationHelper`.
- Guardar con servicio.
- Mostrar `toastr.success` y navegar a la lista.
