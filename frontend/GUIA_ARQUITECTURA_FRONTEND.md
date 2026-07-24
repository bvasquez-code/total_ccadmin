# Guía de arquitectura Frontend

## Propósito

Esta es la versión operativa y resumida de las convenciones del frontend.

La referencia extensa se mantiene en:

- `web_ccadmin_lt/docs/guia-arquitectura-frontend-angular.md`

## Plataforma

- Angular 15.
- TypeScript 4.9.
- Aplicación monolítica registrada en `AppModule`.
- Rutas centralizadas en `AppRoutingModule`.
- Bootstrap 4, Ace Admin y FontAwesome.
- `ngx-toastr`, SweetAlert2 y `xlsx` según el flujo.

No introducir lazy loading, módulos funcionales independientes, otro sistema de
estilos o formularios reactivos salvo que el usuario solicite ese refactor.

## Organización

```text
src/app/enterprise/<module>/
  pages/
    list<entity>/
    create<entity>/
    view<entity>/
  service/
  model/
    entity/
    dto/
    constants/
```

Los elementos reutilizados por distintos módulos se ubican en
`enterprise/shared`.

## Componentes

- Clase con sufijo `Component`.
- Mantener `.component.ts`, `.component.html` y `.component.css` cuando aplique.
- El componente administra estado de pantalla, interacción y presentación.
- No llamar `HttpClient` ni construir comunicaciones HTTP directamente.
- Delegar las llamadas al servicio del dominio.
- Reutilizar componentes, estilos y patrones de una pantalla cercana antes de
  crear otra solución.
- Usar `ngModel`, `ViewChild`, `Input`, `Output` y `EventEmitter` según los
  patrones actuales.

## Servicios y comunicación

- Ubicar servicios en `enterprise/<module>/service`.
- Usar `AppSetting.API`.
- Usar `ApiService.ExecuteGetService` y `ExecutePostService`.
- Mantener endpoints:

```text
${AppSetting.API}/api/v1/<resource>/<action>
```

- Retornar y procesar `ResponseWsDto`.
- Revisar `ErrorStatus` antes de consumir `Data` o `DataAdditional`.
- No duplicar DTOs compartidos.

## Modelos

- Entities con sufijo `Entity`.
- DTOs con sufijo `Dto`.
- Constantes en `model/constants`.
- Mantener nombres compatibles con el contrato backend.
- Usar códigos descriptivos para catálogos extensibles.
- Usar letras para estados cerrados cuando el backend defina ese contrato.

## Formularios y validaciones

- Seguir el patrón de formulario cercano.
- Preferir `ngModel` o `ViewChild`, porque son los mecanismos dominantes.
- Reutilizar `ValidationHelper`.
- Mostrar errores con `ToastrService`.
- No guardar si existen errores de formato o validaciones bloqueantes.
- Las reglas de negocio críticas también deben validarse en backend.

## Excel y cargas masivas

- Leer e interpretar archivos Excel en frontend cuando el flujo ya use `xlsx`.
- Enviar al backend un DTO estructurado, no el archivo para volver a
  interpretarlo.
- Validar nombres de hojas, cabeceras, tipos, filas vacías, duplicados y formatos
  antes de enviar.
- Generar un Excel de errores cuando el usuario necesite corregir el archivo.
- Mantener preview paginado para cargas grandes.
- No implementar actualización en vivo si el proceso define refresco manual.

## UI

- Respetar Bootstrap 4 y Ace Admin.
- Mantener la estructura visual existente de `main-content`, `page-content`,
  tarjetas, tablas y botones.
- Usar FontAwesome disponible en el proyecto.
- Usar el mecanismo de confirmación cercano (`app-modalconfirm`,
  `AlertService` o confirmación simple) sin incorporar otra librería.
- Mantener etiquetas y mensajes comprensibles para el usuario.

## Rutas, módulo y menú

Para una página nueva:

1. crearla dentro de `enterprise/<module>/pages`;
2. declararla en `app.module.ts`;
3. registrarla en `app-routing.module.ts`;
4. incorporarla al mecanismo vigente de configuración del menú;
5. asociar el permiso correspondiente;
6. ocultar del menú las rutas auxiliares que solo se abren desde otra pantalla.

Conservar el formato:

```text
enterprise/<module>/pages/<page>
```

## Listados

- Reutilizar la paginación y tablas existentes cuando encajen con el flujo.
- Conservar filtros, carga, estados vacíos y manejo de errores.
- Una bandeja pesada debe consultar páginas al backend.
- No agregar polling automático si el usuario requiere actualización manual.

## Seguridad

- No implementar autenticación alternativa.
- `ApiService` administra el token.
- Los permisos visibles se controlan mediante la configuración de menú.
- El backend continúa siendo la autoridad de seguridad.

## Verificación mínima

Antes de entregar cambios frontend:

1. comprobar imports, declaraciones y rutas;
2. ejecutar el build Angular;
3. revisar errores de TypeScript y templates;
4. verificar manualmente el flujo cuando sea posible;
5. ejecutar `git diff --check`;
6. no cambiar contratos backend de manera implícita.
