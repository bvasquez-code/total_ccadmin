# Instrucciones Frontend

## Política documental

No leer automáticamente:

- `GUIA_ARQUITECTURA_FRONTEND.md`
- `web_ccadmin_lt/docs/guia-arquitectura-frontend-angular.md`

Consultarlas únicamente cuando el usuario lo solicite según la política del
`AGENTS.md` raíz.

## Reglas obligatorias

- Mantener Angular 15 y la estructura monolítica de `AppModule`.
- No introducir lazy loading ni formularios reactivos sin solicitud expresa.
- Crear pantallas dentro de `src/app/enterprise/<module>/pages`.
- Ubicar servicios y modelos dentro del módulo correspondiente.
- Reutilizar elementos de `enterprise/shared` antes de duplicarlos.
- Los componentes no llaman `HttpClient` directamente.
- Usar `AppSetting.API`, `ApiService` y `ResponseWsDto`.
- Revisar `ErrorStatus` antes de consumir respuestas.
- Mantener contratos compatibles con backend.
- Registrar componentes, rutas, menú y permisos cuando corresponda.
- Respetar Bootstrap 4, Ace Admin y los patrones visuales cercanos.
- Reutilizar `ValidationHelper` y mostrar errores con `ToastrService`.
- Para Excel, interpretar con `xlsx` en frontend y enviar DTOs estructurados.
- No aplicar una carga si existen errores bloqueantes de formato.
- Mantener preview paginado para cargas grandes.
- No agregar actualización automática cuando el flujo requiere refresco manual.

## Verificación

- Revisar imports, declaraciones, rutas y permisos.
- Ejecutar el build Angular.
- Revisar templates y TypeScript.
- Ejecutar `git diff --check`.
