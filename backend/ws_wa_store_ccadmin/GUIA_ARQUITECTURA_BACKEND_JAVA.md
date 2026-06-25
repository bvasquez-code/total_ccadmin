# Guia de arquitectura y convenciones - Backend Java

## 1. Resumen general

Este proyecto es un backend Java basado en Spring Boot para administracion de tienda/ventas. La aplicacion expone APIs REST, persiste datos en MySQL mediante Spring Data JPA y usa seguridad stateless con JWT.

La arquitectura general es modular por dominio funcional. Cada modulo agrupa sus propias capas internas: `controller`, `service`, `repository`, `shared`, `model/entity`, `model/entity/id`, `model/dto`, `exception`, `constants` o `util` cuando aplica.

El paquete base es:

```text
com.ccadmin.app
```

La clase de arranque es:

```text
com.ccadmin.app.ApplicationCcadminApplication
```

No es una arquitectura por capas globales puras. Predomina una organizacion por modulo de negocio con responsabilidades separadas dentro de cada modulo.

## 2. Versiones y dependencias

Versiones identificadas:

- Java: `21`
- Spring Boot: `3.2.0`
- Gestor de dependencias: Maven
- Artifact: `com.ccadmin:app:0.0.1-SNAPSHOT`
- Empaquetado: no se declara explicitamente, por defecto Maven usa `jar`

Dependencias principales:

- `spring-boot-starter-web`: APIs REST.
- `spring-boot-starter-data-jpa`: persistencia JPA.
- `spring-boot-starter-security`: seguridad.
- `mysql-connector-j`: driver MySQL en runtime.
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` version `0.11.5`: creacion y validacion de JWT.
- `lombok` version `1.18.30`: usado en algunas clases, por ejemplo `@Slf4j` y `@RequiredArgsConstructor`.
- `spring-boot-starter-test`: pruebas.

Plugins Maven:

- `spring-boot-maven-plugin`.
- `maven-compiler-plugin` con `source` y `target` en `21`.

## 3. Estructura de paquetes

Paquete base:

```text
src/main/java/com/ccadmin/app
```

Modulos funcionales identificados:

```text
cash
client
payment
person
product
promotion
pucharse
sale
security
shared
store
supplier
system
transfer
user
```

Subestructura recurrente por modulo:

```text
<module>/
  controller/
  service/
  shared/
  repository/
  model/
    entity/
      id/
    dto/
    constants/
    constant/
    myconst/
  exception/
  config/
  util/
  utility/
```

No todos los modulos tienen todas las carpetas. Se crean segun necesidad.

Ejemplos:

- `sale`: modulo transaccional amplio con controllers, services, repositories, entities, DTOs, IDs compuestos, constants y exceptions.
- `system`: modulo de maestros/configuracion como `Currency`, `PaymentMethod`, `Counterfoil`, `AppFile`.
- `shared`: componentes reutilizables como `ResponseWsDto`, `SearchDto`, `AuditTableEntity`, `SessionService`, `SearchTService`, `CcAdminRepository`.
- `security`: configuracion, filtros JWT, services de usuario y entidades de sesion/perfil.

## 4. Convenciones de nombres

Controllers:

- Sufijo `Controller`.
- Nombre basado en la entidad o proceso funcional.
- Ejemplos: `SaleController`, `PresaleController`, `CreditNoteController`, `PaymentMethodController`, `CurrencyController`, `TrxPaymentController`.

Services:

- Sufijo `Service`.
- Para modulos recientes o normalizados se separa por accion:
  - `*CreateService`: operaciones de creacion, guardado, confirmacion, activacion/desactivacion y cambios de estado.
  - `*SearchService`: busquedas, consultas, carga de formularios y lectura.
  - `*TaskService`: tareas asincronas o encoladas.
  - Services con nombre especifico cuando la responsabilidad es especializada.
- Ejemplos: `SaleCreateService`, `SaleSearchService`, `SalePaymentCreateService`, `SalePaymentSearchService`, `PaymentMethodCreateService`, `PaymentMethodSearchService`, `CounterfoilCreateService`, `CounterfoilSearchService`.

Repositories:

- Sufijo `Repository`.
- Extienden normalmente `JpaRepository<Entity, ID>`.
- Algunos tambien implementan `CcAdminRepository<Entity, ID>` para busquedas paginadas por texto.
- Ejemplos: `SaleHeadRepository`, `SaleDetRepository`, `PaymentMethodRepository`, `TrxPaymentRepository`.

Entities:

- Sufijo `Entity`.
- Se ubican bajo `model/entity`.
- Ejemplos: `SaleHeadEntity`, `SaleDetEntity`, `PaymentMethodEntity`, `BusinessConfigEntity`.

IDs compuestos:

- Se ubican bajo `model/entity/id`.
- Usan sufijos `ID`, `Id` o nombres similares; no hay una unica capitalizacion estricta.
- Ejemplos: `SaleDetID`, `SalePaymentID`, `BusinessConfigEntityID`, `PucharseDetId`.

DTOs:

- Sufijo `Dto`.
- Se ubican bajo `model/dto`.
- Ejemplos: `SaleRegisterDto`, `SaleDetailDto`, `SaleConfirmDto`, `SalePaymentRegisterDto`, `ResponseWsDto`, `SearchDto`.

Excepciones:

- Sufijo `Exception`.
- Para errores de construccion/validacion de entidades se usa tambien `*BuildException`.
- Ejemplos: `SaleException`, `SaleBuildException`, `PresaleBuildException`, `TrxPaymentBuildException`.

Metodos frecuentes:

- Busqueda: `findById`, `findAll`, `findDataForm`, `findActives`, `findByDocumentCod`, `findDataPrint`.
- Escritura: `save`, `saveAll`, `confirm`, `enable`, `disable`, `saveClientSale`.
- Entidades: `build`, `validate`, `session`, `active`, `inactive`.

Variables y campos:

- Se observa uso frecuente de nombres con inicial mayuscula en campos publicos: `SaleCod`, `PaymentMethodCod`, `Name`, `Status`, `CreationUser`.
- Los DTOs y entities usan campos publicos en muchos casos, no getters/setters como convencion dominante.
- Los nombres de codigos funcionales suelen terminar en `Cod`.
- Los campos numericos monetarios/cantidades suelen usar prefijo `Num`: `NumUnit`, `NumTotalPrice`, `NumDiscount`.

## 5. Arquitectura por capas o modulos

La arquitectura efectiva es por modulos funcionales. Cada modulo concentra las clases relacionadas con su dominio.

Flujo tipico:

```text
Controller
  -> CreateService / SearchService
      -> Repository
      -> Entity / DTO
      -> Shared de otros modulos cuando aplica
```

Componentes transversales:

- `shared/model/dto`: respuestas y paginacion.
- `shared/model/entity`: auditoria comun.
- `shared/service`: sesion, busqueda generica y cola generica.
- `shared/interfaceccadmin`: contrato de busqueda paginada.
- `system/shared`: helpers de maestros/configuracion/documentos.
- `security`: JWT y autenticacion.

El proyecto permite comunicacion entre services y shared de distintos modulos. Por ejemplo, `SaleCreateService` usa `CounterfoilShared`, `KardexShared`, `ProductRankingService`, repositories propios y `SaleSearchService`.

## 6. Controllers

Los controllers usan:

- `@RestController`
- `@RequestMapping("api/v1/<ruta>")`
- `@GetMapping("<accion>")`
- `@PostMapping("<accion>")`
- `@RequestParam` para parametros simples.
- `@RequestBody` para cuerpos JSON.
- `ResponseEntity<ResponseWsDto>` como retorno dominante.

Convencion de rutas:

```text
api/v1/<modulo-o-recurso>/<accion>
```

Ejemplos:

```text
api/v1/sale/findById
api/v1/sale/findAll
api/v1/sale/confirm
api/v1/paymentMethod/save
api/v1/paymentMethod/enable
api/v1/paymentMethod/disable
```

Manejo de respuesta:

- En exito, se retorna `HttpStatus.OK`.
- En error capturado, se retorna `HttpStatus.BAD_REQUEST`.
- El cuerpo siempre tiende a envolverse en `ResponseWsDto`.

Manejo de errores:

- Los controllers usan `try/catch` local por endpoint.
- En el `catch`, construyen `new ResponseWsDto(ex)`.
- Algunos controllers registran log con Log4j o Lombok `@Slf4j`.
- No se identifico `@ControllerAdvice` como convencion activa.

No identificado en el proyecto analizado:

- Uso sistematico de `@PutMapping`, `@DeleteMapping` o REST semantico por recurso.
- Un handler global de errores con `@ControllerAdvice`.

## 7. Services

Los services usan `@Service` y dependencias inyectadas con `@Autowired` en la mayoria de clases revisadas.

Patrones principales:

- `*CreateService`: contiene operaciones de escritura, construccion de entities, cambios de estado, confirmaciones y transacciones.
- `*SearchService`: contiene busquedas, consultas por ID, listados paginados y `findDataForm`.
- `*Shared`: expone acceso reutilizable para otros modulos sin pasar directamente por controllers.
- Services legacy o especificos: algunos modulos aun tienen services no separados estrictamente, por ejemplo `PaymentMethodService`, `CurrencyService`, `PucharseService`, `PersonService`, segun la evolucion del proyecto.

Transacciones:

- Se usa `jakarta.transaction.Transactional` en metodos de escritura complejos.
- Ejemplo: guardado y confirmacion de ventas en `SaleCreateService`.

Sesion y auditoria:

- Services que necesitan usuario/tienda suelen extender `SessionService`.
- `SessionService` obtiene el usuario desde `SecurityContextHolder`.
- Si no hay usuario autenticado, retorna `"SISTEMA"`.

Responsabilidades observadas:

- El controller no deberia contener logica de negocio.
- El service valida precondiciones de flujo y coordina repositories.
- La entity puede validar sus propios campos con `validate()`.
- La generacion de documentos/correlativos se centraliza en services/shared del modulo `system`.

## 8. Repositories

Los repositories son interfaces de Spring Data JPA.

Patron base:

```java
public interface XRepository extends JpaRepository<XEntity, ID> {
}
```

Para busqueda paginada por texto:

```java
public interface XRepository extends JpaRepository<XEntity, ID>,
        CcAdminRepository<XEntity, ID> {
}
```

`CcAdminRepository` define metodos a sobrescribir:

- `countByQueryText`
- `countByQueryTextStore`
- `findByQueryText`
- `findByQueryTextStore`

Las queries filtradas suelen implementarse con:

- `@Query`
- `nativeQuery = true`
- parametros con `@Param`
- `limit :init, :limit` para paginacion MySQL.

La paginacion se calcula en `SearchDto` y se responde con `ResponsePageSearchT` o `ResponsePageSearch`.

Se usan tambien metodos derivados de Spring Data cuando aplica, por ejemplo `findById`, `existsById`, `save`, `saveAll`.

Projections:

- Existe al menos una interfaz tipo projection: `IExpectedTotalsDto`.
- No se identifica como patron dominante general. Predominan entities, DTOs manuales y queries nativas.

## 9. Entities

Las entities usan Jakarta Persistence:

- `@Entity`
- `@Table(name = "...")`
- `@Id`
- `@IdClass(...)` para claves compuestas.
- `@Transient` para campos no persistidos.
- `@MappedSuperclass` en entidades base.

Campos:

- Se usan campos publicos.
- En muchos casos no se usan `@Column`; se confia en nombres fisicos iguales a la base de datos.
- La configuracion JPA usa `PhysicalNamingStrategyStandardImpl`, por lo que los nombres de campos Java se preservan frente a la base de datos.

Clase base de auditoria:

```text
shared/model/entity/AuditTableEntity
```

Campos comunes:

- `CreationUser`
- `CreationDate`
- `ModifyUser`
- `ModifyDate`
- `Status`

Metodos comunes:

- `addSessionCreate`
- `addSessionModify`
- `addSession`
- `session`
- `inactive`
- `active`

Estados:

- `Status = "A"` para activo.
- `Status = "I"` para inactivo.
- Existe `StatusConst` para constantes de estado de negocio.

Validacion y construccion:

- Varias entities tienen metodos `build(...)`, `validate()` y `session(...)`.
- `validate()` retorna la misma entity para permitir chaining.
- `session(userCod)` retorna la entity concreta cuando se sobreescribe.

Claves primarias:

- Claves simples con `@Id`.
- Claves compuestas con `@IdClass`.
- Los codigos funcionales suelen terminar en `Cod`.
- En detalles transaccionales se usan claves compuestas con campos como codigo de cabecera e `ItemNumber`.

Relaciones:

- Se identifican imports y uso de anotaciones JPA, pero en la muestra revisada predominan relaciones manuales por campos `*Cod` y campos `@Transient` para hidratar objetos relacionados.
- No se identifica como convencion dominante el uso intensivo de `@ManyToOne` / `@OneToMany`.

## 10. DTOs, requests y responses

DTO estandar de respuesta:

```text
ResponseWsDto
```

Estructura:

- `Status`
- `Message`
- `Data`
- `ErrorStatus`
- `ErrorID`
- `DataAdditional`

Comportamiento:

- Constructor con `Object`: marca respuesta OK y asigna `Data`.
- Constructor con `Exception`: marca error, registra log y usa `ex.getMessage()` como mensaje.
- `okResponse(Object Data)`: helper fluido para respuesta OK.
- `errorResponse(Exception ex)`: helper fluido para error.
- `AddResponseAdditional(String name, Object data)`: agrega datos adicionales para formularios/catalogos.

Paginacion:

- `SearchDto`: contiene `Query`, `Page`, `StoreCod`, `Limit`, `Init`.
- `ResponsePageSearch`: respuesta paginada no generica.
- `ResponsePageSearchT<T>`: respuesta paginada generica.

Requests:

- No hay sufijo unico obligatorio `Request`.
- Se usan DTOs con sufijos como `RegisterDto`, `ConfirmDto`, `DetailDto`, `SearchDto`.
- En algunos maestros se recibe directamente la entity como `@RequestBody`, por ejemplo `PaymentMethodEntity`.

Convencion de campos:

- Campos publicos.
- Nombres con inicial mayuscula en muchos DTOs/entities.
- Nombres alineados con columnas o conceptos de base de datos.

## 11. Validaciones

Las validaciones se hacen principalmente de forma manual.

Ubicaciones:

- En services: validaciones de flujo, null checks, existencia de registros, estados y reglas de negocio.
- En entities: validaciones de campos internos mediante `validate()`.
- En metodos `build(...)`: armado controlado de entities a partir de DTOs u otras entities.

Ejemplos de validacion:

- `PaymentMethodCreateService.save(...)` valida que el objeto no sea null, llama `paymentMethod.validate()` y luego guarda.
- `PaymentMethodEntity.validate()` valida que `PaymentMethodCod` exista.
- `SaleCreateService.save(...)` valida cabecera y detalle antes de construir entities.
- `SaleDetEntity.validate()` valida codigo, cantidades y montos no negativos.

No identificado en el proyecto analizado:

- Uso sistematico de Bean Validation con `@Valid`, `@NotNull`, `@NotBlank`, etc.
- Un mecanismo centralizado de mensajes de validacion.

## 12. Manejo de errores

Tipos de excepciones:

- Excepciones checked de negocio: por ejemplo `SaleException`.
- Excepciones runtime de construccion/validacion: por ejemplo `SaleBuildException`.
- Excepciones estandar: `IllegalArgumentException`.

Propagacion:

- Services lanzan excepciones con mensajes orientados al frontend.
- Controllers capturan `Exception` de forma amplia.
- En error, se devuelve `ResponseWsDto(ex)` con `HttpStatus.BAD_REQUEST`.

Logs:

- Algunos controllers y services usan `LogManager`.
- Algunas clases usan Lombok `@Slf4j`.
- `ResponseWsDto(Exception ex)` tambien registra el error.

No identificado en el proyecto analizado:

- `@ControllerAdvice`.
- Jerarquia global unica de errores.
- Mapeo centralizado de excepciones a codigos HTTP.

## 13. Seguridad y sesion

Seguridad:

- Spring Security esta habilitado con `@EnableWebSecurity`.
- La API es stateless mediante `SessionCreationPolicy.STATELESS`.
- CSRF esta deshabilitado.
- `/login` y `/public/**` son publicos.
- El resto de rutas requiere autenticacion.
- Se permite `OPTIONS /**` para preflight CORS.

JWT:

- `JWTAuthenticationFilterService` procesa login en `/login`.
- `JWTAuthorizationFilterService` lee el header `Authorization`.
- El formato esperado es `Bearer <token>`.
- `TokenUtil` crea y valida tokens JWT con JJWT.
- El subject del token es `userCod`.

Sesion de aplicacion:

- `SessionService.getUserCod()` lee el principal desde `SecurityContextHolder`.
- Si falla la lectura, retorna `"SISTEMA"`.
- `SessionService.getStoreCod()` obtiene la tienda principal mediante `UserStoreShared`.

Auditoria:

- Las entities que extienden `AuditTableEntity` registran usuario y fecha en creacion/modificacion mediante `addSession(...)`.
- Los services de escritura deben llamar `session(...)`, `addSession(...)`, `active(...)` o `inactive(...)` segun corresponda.

Observacion:

- El secreto JWT esta hardcodeado en `TokenUtil`. Esto existe en el proyecto actual, pero no deberia copiarse como buena practica a nuevos proyectos sin parametrizarlo.

## 14. Configuracion

Archivos:

```text
src/main/resources/application.properties
src/main/resources/application-dev.properties
src/main/resources/logback-spring.xml
```

Perfil activo:

```properties
spring.profiles.active=dev
```

Configuracion dev:

```properties
server.address=0.0.0.0
server.port=8090
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/db_store_01
spring.datasource.username=root
spring.datasource.password=...
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
spring.jpa.show-sql=false
```

CORS:

- Se configura en `WebSecurityConfig` mediante `CorsConfigurationSource`.
- Permite origen `*`.
- Permite metodos `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS`.
- Permite headers `*`.
- Expone `Authorization` y `Content-Type`.
- `allowCredentials=false`.

Logs:

- `logback-spring.xml` configura consola con patron:

```text
%d{yyyy-MM-dd HH:mm:ss} - [%level] - [%logger{36}:%line] %msg %n
```

Swagger/OpenAPI:

- No identificado en el proyecto analizado.

## 15. Reglas para nuevos desarrollos Backend Java

Para crear un nuevo modulo, seguir la estructura modular existente. No crear una arquitectura paralela.

Carpetas recomendadas:

```text
src/main/java/com/ccadmin/app/<module>/
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

Crear controller:

- Nombrar como `<EntityOrProcess>Controller`.
- Anotar con `@RestController`.
- Usar `@RequestMapping("api/v1/<resource>")`.
- Retornar `ResponseEntity<ResponseWsDto>`.
- Envolver exito con `new ResponseWsDto(data)` o `new ResponseWsDto().okResponse(data)`.
- Capturar errores con `try/catch` y devolver `new ResponseWsDto(ex)` con `HttpStatus.BAD_REQUEST`.

Crear services:

- Separar lectura y escritura cuando el modulo tenga CRUD o flujo claro.
- Usar `<EntityOrProcess>CreateService` para `save`, `saveAll`, `confirm`, `enable`, `disable` y cambios de estado.
- Usar `<EntityOrProcess>SearchService` para `findById`, `findAll`, `findDataForm`, `findActives` y consultas.
- Extender `SessionService` en services que escriben auditoria o necesitan usuario/tienda.
- Usar `@Transactional` en operaciones que guardan varias tablas o cambian estados relacionados.
- Reutilizar `Shared` existentes para funciones transversales.

Crear repositories:

- Nombrar como `<Entity>Repository`.
- Extender `JpaRepository<Entity, ID>`.
- Si requiere busqueda paginada textual, extender tambien `CcAdminRepository<Entity, ID>`.
- Implementar `countByQueryText` y `findByQueryText` con `@Query(nativeQuery = true)`.
- Para busqueda por tienda, implementar `countByQueryTextStore` y `findByQueryTextStore`.

Crear entities:

- Nombrar como `<Entity>Entity`.
- Ubicar en `model/entity`.
- Usar `@Entity` y `@Table(name = "<tabla>")`.
- Usar `@Id` para clave simple.
- Usar `@IdClass` y clase en `model/entity/id` para clave compuesta.
- Extender `AuditTableEntity` si la tabla tiene campos de auditoria y `Status`.
- Mantener campos publicos si se sigue el estilo actual del proyecto.
- Agregar `build(...)`, `validate()` y `session(...)` cuando la entity participe en flujos de negocio.
- Usar `active(userCod)` e `inactive(userCod)` para activar/desactivar; no eliminar fisicamente si el dominio maneja `Status`.

Crear DTOs:

- Nombrar con sufijo `Dto`.
- Usar nombres segun el flujo: `RegisterDto`, `DetailDto`, `ConfirmDto`, `SearchDto`.
- Ubicar en `model/dto`.
- Mantener estructura de campos compatible con los DTOs existentes.

Estructurar endpoints:

- Usar rutas por accion, segun el estilo actual:

```text
GET  api/v1/<resource>/findById
GET  api/v1/<resource>/findAll
GET  api/v1/<resource>/findDataForm
GET  api/v1/<resource>/findActives
POST api/v1/<resource>/save
POST api/v1/<resource>/saveAll
POST api/v1/<resource>/enable
POST api/v1/<resource>/disable
POST api/v1/<resource>/confirm
```

Devolver respuestas:

- Usar siempre `ResponseWsDto` en controllers.
- Usar `Data` para el resultado principal.
- Usar `DataAdditional` para catalogos o datos auxiliares de formularios.
- Usar `ResponsePageSearchT<T>` para listados paginados.

Manejar errores:

- Lanzar excepciones desde services con mensajes claros.
- Usar excepciones propias cuando el modulo ya tenga una familia de excepciones.
- Usar `IllegalArgumentException` para validaciones simples si no existe excepcion propia.
- Capturar en controller y responder con `ResponseWsDto(ex)`.

Validar:

- Validar precondiciones de flujo en el service.
- Validar campos internos en `entity.validate()`.
- Hacer que `validate()` retorne la misma entity para permitir chaining.
- Llamar `validate()` antes de persistir.

Seguir la arquitectura existente:

- No mover logica de negocio al controller.
- No consultar repositories directamente desde controllers.
- No crear respuestas REST nuevas si `ResponseWsDto` cubre el caso.
- No crear otro sistema de paginacion si `SearchDto` + `SearchTService` + `ResponsePageSearchT` aplican.
- No eliminar fisicamente maestros con `Status`; usar `enable` y `disable`.
- No duplicar logica transversal; crear o reutilizar clases `Shared` cuando otro modulo necesite la misma capacidad.

## 16. Ejemplo de estructura recomendada para un nuevo modulo

Ejemplo para un recurso `Example` dentro del modulo `example`:

```text
src/main/java/com/ccadmin/app/example/
  controller/
    ExampleController.java
  service/
    ExampleCreateService.java
    ExampleSearchService.java
  shared/
    ExampleShared.java
  repository/
    ExampleRepository.java
  model/
    entity/
      ExampleEntity.java
      id/
        ExampleID.java
    dto/
      ExampleRegisterDto.java
      ExampleDetailDto.java
    constants/
      ExampleConstants.java
  exception/
    ExampleException.java
    ExampleBuildException.java
```

Esqueleto conceptual:

```text
ExampleController
  GET  api/v1/example/findById
  GET  api/v1/example/findAll
  GET  api/v1/example/findDataForm
  POST api/v1/example/save
  POST api/v1/example/enable
  POST api/v1/example/disable

ExampleCreateService extends SessionService
  save(...)
  saveAll(...)
  enable(...)
  disable(...)

ExampleSearchService
  findById(...)
  findAll(query, page)
  findDataForm(...)
  findAllActive(...)

ExampleRepository extends JpaRepository<ExampleEntity, String>,
    CcAdminRepository<ExampleEntity, String>

ExampleEntity extends AuditTableEntity
  @Entity
  @Table(name = "example")
  @Id public String ExampleCod
  public ExampleEntity validate()
  public ExampleEntity session(String userCod)
```

Este ejemplo es una estructura recomendada derivada de los patrones existentes. Los nombres reales deben adaptarse al dominio del nuevo modulo y a las tablas reales de base de datos.
