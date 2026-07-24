# Guía de arquitectura Backend

## Propósito

Esta es la versión operativa y resumida de las convenciones del backend.
Conserva las decisiones necesarias para desarrollar y revisar código sin repetir
la descripción histórica completa del proyecto.

La referencia extensa se mantiene en:

- `ws_wa_store_ccadmin/GUIA_ARQUITECTURA_BACKEND_JAVA.md`

Si existe una diferencia, una decisión explícita posterior del usuario tiene
prioridad y debe incorporarse luego en esta guía.

## Plataforma

- Java 21.
- Spring Boot 3.2.
- Maven.
- Spring Web, Spring Data JPA y Spring Security.
- MySQL.
- API stateless con JWT.
- Paquete base: `com.ccadmin.app`.

## Organización

La aplicación se organiza por dominio funcional, no mediante capas globales:

```text
com/ccadmin/app/<module>/
  controller/
  service/
  shared/
  repository/
  model/
    entity/
      id/
    dto/
    constants/
  exception/
```

Crear únicamente las carpetas requeridas. No introducir una arquitectura
paralela a la existente.

El flujo habitual es:

```text
Controller
  -> CreateService / SearchService
      -> Repository
      -> Entity / DTO
      -> Shared de otro dominio, cuando corresponda
```

## Servicios

Separar responsabilidades cuando el recurso tenga lectura y escritura:

- `*CreateService`: creación, edición, confirmación, resolución, activación,
  anulación y demás comandos.
- `*SearchService`: búsquedas, bandejas, consulta por ID y datos de formularios.
- `*TaskService`: ejecución asíncrona o encolada.
- Un servicio con nombre especializado cuando represente una capacidad de
  negocio concreta.

Los métodos públicos de escritura deben actuar como orquestadores legibles.
Deben mostrar con claridad las fases principales: validar, construir,
persistir, ejecutar la operación de negocio y devolver el resultado.

### Reutilización obligatoria

Antes de implementar un flujo nuevo:

1. localizar el flujo de negocio equivalente;
2. identificar validaciones, persistencia, auditoría y efectos secundarios;
3. extraer un núcleo común cuando el flujo normal y el nuevo hacen lo mismo;
4. hacer que ambos caminos deleguen en ese núcleo.

No duplicar lógica de:

- confirmación;
- Kardex o actualización de stock;
- precios;
- auditoría;
- cambios de estado;
- generación de búsquedas;
- correlativos;
- validaciones de dominio ya existentes.

Los procesos masivos o especializados deben transformar su entrada a un DTO de
dominio y delegar. No deben reconstruir por su cuenta el proceso de negocio.

Preferir operaciones de lista como `saveAll(...)` cuando toda la colección forma
parte de la misma operación. Los bucles pueden preparar entidades, pero la
persistencia y confirmación deben estar centralizadas.

## Controllers

- Sufijo `Controller`.
- `@RestController` y `@RequestMapping("api/v1/<resource>")`.
- `@RequestParam` para parámetros simples y `@RequestBody` para JSON.
- Retornar `ResponseEntity<ResponseWsDto>`.
- Mantener los endpoints por acción usados por el proyecto.
- No colocar lógica de negocio ni consultas directas a repositories.
- Delegar consultas en `*SearchService` y comandos en `*CreateService`.
- Traducir las excepciones al formato de respuesta existente.

## Nombres

- Clases en PascalCase.
- Campos y variables locales en lowerCamelCase, salvo los campos públicos de
  entities y DTOs que reflejan nombres funcionales como `ProductCod`.
- Usar nombres descriptivos completos para dependencias:

```java
stockEntryHeadRepository
stockEntryDetRepository
stockMovementValidationService
productConfigCreateService
```

Evitar nombres ambiguos como `headRepository`, `detRepository`, `service`,
`helper` o `validation` cuando la clase contiene varias dependencias.

- Repositories: `<Entity>Repository`.
- Entities: `<Entity>Entity`.
- DTOs: nombres de flujo con sufijo `Dto`.
- Constantes: clase del dominio con sufijo `Constants`.

## Persistencia, entidades y DTOs

- Repositories extienden normalmente `JpaRepository<Entity, ID>`.
- Usar queries derivadas cuando sean suficientes.
- Usar `@Query(nativeQuery = true)` para consultas específicas del modelo
  existente.
- Las entities se ubican en `model/entity`.
- Usar `@IdClass` y una clase en `model/entity/id` para claves compuestas.
- Conservar campos públicos en entities y DTOs mientras sea la convención del
  módulo.
- Extender `AuditTableEntity` cuando la tabla tenga auditoría y estado lógico.
- Mantener nombres Java alineados con las columnas porque la estrategia física
  preserva sus nombres.

## Transacciones, sesión y auditoría

- Usar `@Transactional` en operaciones que modifican varias tablas, detalles,
  estados o movimientos relacionados.
- La transacción debe cubrir la operación de negocio completa.
- Los servicios interactivos pueden obtener usuario y tienda desde
  `SessionService`.
- Un proceso en segundo plano no debe depender del `SecurityContext`; debe
  recibir `userCod` explícitamente.
- Registrar creación y modificación con los métodos de `AuditTableEntity`.
- Cuando un generador de código administra autocommit propio, solicitar el
  código antes de abrir la transacción que procesa el documento.

## Validaciones y errores

- Validar precondiciones y estados de flujo en el service.
- Reutilizar validadores existentes.
- Usar `validate()` o `build(...)` en entities cuando el módulo ya siga ese
  patrón.
- Lanzar mensajes comprensibles para el frontend.
- Usar excepciones de dominio cuando ya exista una familia para el módulo.
- Usar `IllegalArgumentException` o `IllegalStateException` para reglas simples.

## Integraciones entre dominios

- Usar un `Shared` cuando otro módulo necesite una capacidad pública y estable.
- No convertir `Shared` en un lugar para lógica duplicada.
- Un proceso coordinador puede conocer el tipo de operación para reconstruir el
  DTO, pero la lógica particular debe permanecer en el servicio especializado.

## Verificación mínima

Antes de entregar cambios backend:

1. buscar referencias residuales a clases o métodos reemplazados;
2. compilar con Java 21;
3. ejecutar las pruebas Maven;
4. añadir pruebas para núcleos reutilizados o reglas con riesgo de regresión;
5. ejecutar `git diff --check`;
6. no modificar contratos HTTP sin que el cambio haya sido solicitado.

## Estructura recomendada

```text
<module>/
  controller/
    ExampleController.java
  service/
    ExampleCreateService.java
    ExampleSearchService.java
  repository/
    ExampleRepository.java
  model/
    entity/
      ExampleEntity.java
      id/
    dto/
      ExampleRegisterDto.java
      ExampleSearchDto.java
    constants/
      ExampleConstants.java
  shared/
  exception/
```
