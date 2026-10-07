# Preparar una instalación limpia de tienda

El flujo es: **exportar la base actual completa → crear `db_store_lts` → importar la copia → limpiar la copia → verificar → exportar la plantilla final**. La base actual continúa con todos sus datos. Estos archivos preparan el proceso; no se ha ejecutado sobre tu servidor ni se incluye un dump completo de tu base.

Los archivos que compartiste contienen los `INSERT` de `store`, `company`, `counterfoil` y `counterfoil_store`. Se usan como referencia para las semillas iniciales, no como respaldo completo. Los nombres `STORE_DEFAULT` y `COMPANY_DEFAULT` activan la configuración inicial que el backend ofrece a `ROOT`.

## Archivos y resultado

| Archivo | Uso |
| --- | --- |
| `01_crear_db_store_lts.sql` | Crear una base nueva, sin sobrescribir una existente. |
| `02_limpiar_db_store_lts.sql` | Limpiar exclusivamente la copia seleccionada `db_store_lts`. |
| `03_verificar_estado_inicial.sql` | Consultar el estado final, sin mostrar contraseñas. |

El script clasifica las 114 tablas actuales del repositorio y revisa las tablas instaladas mediante `information_schema`. Una tabla desconocida cancela la ejecución antes de borrar datos. Las tablas operativas opcionales y el catálogo estático `worldcities` pueden faltar; las de configuración y las necesarias para reconstruir el estado inicial deben existir.

| Datos | Resultado |
| --- | --- |
| Productos | Solo `D0000001` (delivery). Su registro, variantes, códigos de barras, imágenes, marca, categoría y ancestros se conservan **sin modificar sus campos**. Elimina los demás productos y las dependencias fuera de las excepciones descritas. |
| Marca y categorías genéricas | Conserva también la marca con `BrandName=GENERICO` y la categoría padre con `CategoryName=GENERICO`, **todos sus hijos y descendientes**, y los ancestros necesarios para mantener la jerarquía. Identifica el padre por `IsCategoryDad=S` o por no tener padre. No modifica campos, estados ni auditoría de los registros conservados. |
| Configuración de delivery | Conserva sin cambios los registros de `product_config` y `product_tax_config` de `D0000001`/`T001`. Elimina configuraciones de las tiendas eliminadas. Deben existir el producto, sus variantes y su configuración activa en `T001` antes de limpiar; si faltan, cancela sin inventarlos. |
| Stock, búsqueda e historial de productos | Reinicia stock, kardex, lotes, rankings e historial de precios. El trigger de tienda reconstruye stock cero y búsqueda de delivery en `T001` y `T0010001`, usando las definiciones conservadas. |
| Compras, ventas, preventas, transferencias, notas de crédito, pagos y documentos | Vacíos, incluidos detalles, solicitudes, cargas masivas y registros SUNAT. |
| Clientes, cuentas web, direcciones, proveedores, transportistas, promociones | Vacíos. |
| Usuarios y personas | Solo `ROOT`, `USER_WEB` y sus personas; conserva sus contraseñas actuales y perfiles asignados. Elimina códigos de recuperación y `PasswordDecoded`. |
| Sesiones, historial, errores y documentos del sistema | Vacíos. |
| Archivos e imágenes | Conserva `app_file` referenciados por `payment_method.FileCod` **o por coincidencia de `Route`**, además de las imágenes de `D0000001`. Elimina solo los demás registros de archivos. No elimina archivos físicos del disco. |
| Compañía | Solo `C001`, `COMPANY_DEFAULT`, RUC de referencia `20000000001`, dirección `Av. Principal 123`, ubigeo `140101`, país `PE`. |
| Tienda | Solo `T001`, `STORE_DEFAULT`, vinculada a `C001`, país `PER`, sin dirección ni coordenadas, tienda virtual desactivada. |
| Periodos | Solo `PeriodId=1`, código `INICIAL`, activo, sin fechas específicas de una instalación anterior. |
| Almacenes y cajas | Almacén `T0010001` y una caja por cada usuario por defecto, creados por los triggers y procedimientos existentes. Ambos usuarios quedan asociados a `T001` como tienda principal. |
| Talonarios y sus asignaciones | Carga exactamente `01F001`, `03B001`, `07B001`, `07F001`, `09T001`, todos asociados a `T001`, con correlativo **0**. Conserva los valores de las semillas adjuntas, incluidas sus fechas de auditoría. Elimina talonarios anteriores. |
| SUNAT y Mercado Pago | Configuración SUNAT vacía. En `business_config` solo vacía `Str3Config` (Access Token privado) de `MercadoPagoEcommerce`/`MercadoPagoCheckout`. Conserva `ConfigVal` (Public Key) y todos sus otros campos, incluidos modo, estado y auditoría. |
| Delivery web | Configuración virtual vacía y tienda virtual desactivada. **Tarifas, horarios y demás filas de `business_config` permanecen exactamente como estaban**. |
| Catálogos del sistema | Conserva íntegros `worldcities`, `city`, `country`, `state`, `ubigeo_department`, `ubigeo_province` y `ubigeo_district`, además de monedas, impuestos, afectaciones, canales y tipos de entrega. No elimina ni modifica datos geográficos. `payment_method` queda **íntegra**, incluidos `FileCod`, `Route` y auditoría. |
| Menús, perfiles y configuración general | Conserva menús, **todos los perfiles** y permisos. `business_config`, sus grupos y sus definiciones se conservan salvo el campo del token privado indicado. |

### Correlativos y validaciones

- Usa `TRUNCATE` en las tablas que se vacían completamente o se reconstruyen con semillas. En tablas con registros protegidos usa **`DELETE` selectivo**, sin truncar ni reinsertar esos registros. Las tablas marcadas `K` se conservan, con la única excepción documentada del token privado de Mercado Pago.
- `TRUNCATE` reinicia `AUTO_INCREMENT`; en tablas de limpieza selectiva con identidad se ajusta al mínimo seguro `MAX(ID conservado)+1`. `period` termina con el siguiente ID en 2 porque ya existe el periodo inicial 1.
- Conserva las definiciones de secuencias globales. Todas se reinician; las de tablas vacías quedan en **0** y las de maestros conservados/recreados se ajustan al **mínimo seguro de sus códigos existentes**. Por ejemplo, si la definición genera `C001`/`T001`, esas secuencias terminan en 1; con el formato habitual de caja de ocho dígitos, `cash_register` termina en 2. Ponerlas en 0 volvería a generar códigos ya utilizados.
- Conserva una plantilla de prefijo y longitud por tipo de secuencia de tienda, prefiriendo `T001` y su periodo activo original. Recrea todas para `T001`/periodo 1 con correlativo **0**. Requiere ocho definiciones: compras y solicitudes de compra, entradas y salidas de stock, transferencias, preventas, ventas y notas de crédito. `transfer_request_head` se conserva y reinicia solo si ya tiene una definición; no se crea una adicional cuando falta. Si falta otra definición requerida, informa su nombre sin inventar prefijos.
- La ausencia de `transfer_request_head` se permite para conservar la configuración de la copia. El backend del repositorio solicita esa secuencia en `TransferRequestHeadRepository.getTransferCod` cuando reserva un código para una solicitud independiente; ese flujo requiere su definición. La limpieza no convierte dos secuencias en una compartida ni cambia la generación de códigos del backend.
- Desactiva `FOREIGN_KEY_CHECKS` **solo en la conexión de limpieza**, y lo restaura al terminar o ante un error SQL. Mantiene los triggers, claves únicas y `CHECK`: se necesitan para crear y validar las semillas.
- Al finalizar, cuenta cada tabla y verifica todas las claves foráneas, incluidas las compuestas. Reactivar las FK por sí solo no comprueba los datos existentes.
- `TRUNCATE` hace commits implícitos; **no se puede recuperar con `ROLLBACK`**. Si falla después de iniciar la limpieza, no exportar: volver a crear e importar la copia desde el respaldo. Una ejecución completada sí se puede repetir.

## 1. Preparar los clientes y detener escrituras

Necesitas `mysql` y `mysqldump`, preferiblemente de la versión del servidor. El SQL está escrito para MySQL 8. En Windows, abre PowerShell y adapta estas variables; la ruta 9.5 corresponde al cliente encontrado en esta máquina, pero puede ser distinta en tu entorno:

```powershell
$MysqlBin = 'C:\Program Files\MySQL\MySQL Server 9.5\bin'
$MysqlExe = Join-Path $MysqlBin 'mysql.exe'
$MysqlDumpExe = Join-Path $MysqlBin 'mysqldump.exe'
$MysqlSourceOptions = @()
if ((& $MysqlExe --no-defaults --help | Out-String) -match '(?m)^\s+--commands\s') {
    $MysqlSourceOptions = @('--commands=ON')
}
$MysqlHost = '127.0.0.1'
$MysqlPort = 3306
$MysqlUser = 'root'
$OriginalDatabase = 'REEMPLAZAR_POR_NOMBRE_BD_ACTUAL'
$BackupDirectory = 'C:\respaldos_ccadmin'
$ScriptsDirectory = 'C:\proyectos\multiple\total_ccadmin\database\incialización de tienda'
New-Item -ItemType Directory -Path $BackupDirectory -Force | Out-Null
Set-Location -LiteralPath $ScriptsDirectory

& $MysqlExe --host=$MysqlHost --port=$MysqlPort --user=$MysqlUser --password --execute="SELECT VERSION(); SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME IN ('$OriginalDatabase','db_store_lts');"
```

`db_store_lts` debe ser un nombre nuevo, distinto de la base actual. `01` falla si ya existe: no la reutilices sin comprobar primero qué contiene. No cambies la conexión del backend de trabajo a la copia.

`$MysqlSourceOptions` habilita los comandos de archivos `source` en los clientes recientes que los desactivan por defecto. En clientes que no tienen esa opción, conserva una lista vacía.

Detén temporalmente las aplicaciones/workers que escriben en la base original mientras tomas el respaldo y evita cambios de estructura. `--single-transaction` ofrece una instantánea consistente para InnoDB; si hay tablas no transaccionales, usa una ventana sin escrituras y un respaldo con bloqueo acorde a esas tablas.

Un dump de una base incluye sus objetos y datos; no incluye usuarios/permisos del servidor MySQL, archivos externos ni configuraciones del backend. Las contraseñas se solicitan de forma interactiva.

**Las imágenes referenciadas también requieren sus archivos físicos.** El SQL conserva sus registros y rutas, pero el dump no empaqueta los PNG/JPG del almacenamiento. Incluye en el paquete de instalación los archivos usados por los medios de pago y por delivery, respetando las rutas conservadas o el almacenamiento que configure el backend. Copia también los recursos estáticos referenciados directamente por `payment_method.Route` aunque no tengan fila en `app_file`.

## 2. Exportar la base original completa

```powershell
& $MysqlDumpExe --host=$MysqlHost --port=$MysqlPort --user=$MysqlUser --password --default-character-set=utf8mb4 --single-transaction --routines --triggers --events --hex-blob --no-tablespaces --set-gtid-purged=OFF --result-file="$BackupDirectory\original_completa.sql" $OriginalDatabase
if ($LASTEXITCODE -ne 0) { throw 'Fallo el respaldo; no continuar.' }
Get-Item -LiteralPath "$BackupDirectory\original_completa.sql"
Get-FileHash -LiteralPath "$BackupDirectory\original_completa.sql" -Algorithm SHA256
```

No uses `--databases` ni `--all-databases`: añaden instrucciones para seleccionar/crear la base original. `--result-file` evita problemas de codificación al redirigir un dump con PowerShell. Conserva este respaldo privado y sin modificar, porque contiene datos y credenciales de la instalación actual.

Antes de importar, revisa en **una copia del archivo**:

- No debe haber un `CREATE DATABASE`, `DROP DATABASE` ni `USE` que apunte a la base de trabajo. Si viene de otra herramienta, elimina esas instrucciones de la copia destinada a importar.
- Busca el nombre de la base original en definiciones de vistas, triggers, rutinas, eventos y FK. Ajusta las referencias calificadas de esos objetos a `db_store_lts`; no hagas un reemplazo indiscriminado que altere datos de texto. `--database=db_store_lts` no reescribe referencias explícitas a otro esquema.
- Revisa `DEFINER`: el usuario debe existir y tener permisos en la copia. Para distribuir a otro servidor, prepara las definiciones con un usuario válido de ese entorno.
- Si existen eventos, cambia **en la copia del dump** su `ENABLE` a `DISABLE` antes de importarla. Así no ejecutan trabajos sobre la copia recién creada. El script también rechaza una copia con eventos habilitados; no desactives globalmente el scheduler del servidor de trabajo.

Si necesitas editarlo, guarda la versión revisada como `original_para_copia.sql` y usa ese nombre en el siguiente paso. Si no hay referencias externas ni eventos que ajustar, usa `original_completa.sql`.

## 3. Crear `db_store_lts` e importar todo

```powershell
& $MysqlExe @MysqlSourceOptions --host=$MysqlHost --port=$MysqlPort --user=$MysqlUser --password --default-character-set=utf8mb4 --execute='source 01_crear_db_store_lts.sql'
if ($LASTEXITCODE -ne 0) { throw 'No se creo una base nueva; no continuar.' }

& $MysqlExe @MysqlSourceOptions --host=$MysqlHost --port=$MysqlPort --user=$MysqlUser --password --default-character-set=utf8mb4 --database=db_store_lts --execute="source C:/respaldos_ccadmin/original_completa.sql"
if ($LASTEXITCODE -ne 0) { throw 'Importacion incompleta; no limpiar ni exportar.' }
```

La ruta del `source` de importación corresponde al directorio del ejemplo; adáptala si cambiaste `$BackupDirectory`. El archivo `01` crea la base con `utf8mb4`/`utf8mb4_0900_ai_ci`, igual que las tablas del repositorio.

Conecta tu editor SQL específicamente a `db_store_lts` y verifica antes de limpiar:

```sql
SELECT DATABASE();
SELECT COUNT(*) AS TablasCopiadas FROM information_schema.tables
WHERE table_schema = 'db_store_lts' AND table_type = 'BASE TABLE';
SELECT routine_name, routine_definition FROM information_schema.routines
WHERE routine_schema = 'db_store_lts';
SELECT trigger_name, action_statement FROM information_schema.triggers
WHERE trigger_schema = 'db_store_lts';
SELECT table_name, view_definition FROM information_schema.views
WHERE table_schema = 'db_store_lts';
SELECT event_name, status, event_definition FROM information_schema.events
WHERE event_schema = 'db_store_lts';
SELECT UserCod, PersonCod, Status, DateExpire FROM db_store_lts.app_user
WHERE UserCod IN ('ROOT', 'USER_WEB');
```

Comprueba nuevamente que las definiciones no escriban ni lean la base original, que los eventos estén deshabilitados y que estén las dos cuentas activas y sus personas. El script conserva las contraseñas que ya tienen: no conoce ni establece una contraseña por defecto. Si `ROOT` no tiene un perfil activo con menús, detiene la limpieza.

Revisa también `D0000001`, sus variantes y su configuración activa en `T001`. No ejecutes primero la versión anterior que eliminaba todos los productos: esta versión necesita conservar el delivery original. Si ya limpiaste una copia con la versión anterior, vuelve a importarla desde el respaldo completo.

En este punto puedes reanudar el backend de la base original. Mantén la copia sin aplicaciones conectadas durante limpieza y exportación.

## 4. Limpiar únicamente la copia

Ejecuta el archivo completo en una conexión nueva, con autocommit habilitado. No uses `--force` ni una opción del editor para ignorar errores.

```powershell
& $MysqlExe @MysqlSourceOptions --host=$MysqlHost --port=$MysqlPort --user=$MysqlUser --password --default-character-set=utf8mb4 --database=db_store_lts --execute='source 02_limpiar_db_store_lts.sql'
if ($LASTEXITCODE -ne 0) { throw 'Limpieza fallida; no exportar esta copia.' }
```

Debe devolver `InitializedDatabase=db_store_lts`, `Result=OK` y `ForeignKeyChecks=1`. Todas las tablas marcadas `C` deben mostrar `RemainingRows=0`; las `S` solo pueden contener las excepciones protegidas; las `R` contienen las semillas o datos reconstruidos; cada FK debe mostrar `OrphanRows=0`. El reporte de correlativos globales permite revisar las reservas mínimas para los registros iniciales.

El script solo crea/elimina su procedimiento auxiliar en `db_store_lts`, aunque se lance con otra base seleccionada; en ese caso cancela antes de cambiar datos. Conserva `get_cod_seq`, `get_cod_trx` y los demás objetos de negocio.

## 5. Verificar la plantilla y revisar lo conservado

```powershell
& $MysqlExe @MysqlSourceOptions --host=$MysqlHost --port=$MysqlPort --user=$MysqlUser --password --default-character-set=utf8mb4 --database=db_store_lts --execute='source 03_verificar_estado_inicial.sql'
if ($LASTEXITCODE -ne 0) { throw 'Verificacion fallida; no exportar.' }
```

Revisa los resultados contra la tabla de estado inicial de esta guía. Los catálogos y la configuración general vienen de tu base actual: revisar perfiles personalizados, valores de `business_config`, textos, correos y datos de las personas de `ROOT`/`USER_WEB` antes de distribuir. Solo se eliminan automáticamente las credenciales de integración identificadas en el código actual (SUNAT y Mercado Pago); otros valores personalizados deben revisarse expresamente.

La plantilla hereda la contraseña de `ROOT` y su `DateExpire`. Define una contraseña de instalación y revisa su vigencia antes de distribuir, mediante el mecanismo habitual de administración de usuarios. No configures aún la compañía/tienda ni realices ventas en esta copia: cambiar `COMPANY_DEFAULT` o `STORE_DEFAULT` elimina la condición de primera configuración.

## 6. Exportar el script final para instalaciones nuevas

```powershell
& $MysqlDumpExe --host=$MysqlHost --port=$MysqlPort --user=$MysqlUser --password --default-character-set=utf8mb4 --single-transaction --routines --triggers --events --hex-blob --no-tablespaces --set-gtid-purged=OFF --result-file="$BackupDirectory\db_store_lts_instalacion_inicial.sql" db_store_lts
if ($LASTEXITCODE -ne 0) { throw 'Fallo la exportacion de la plantilla.' }
Get-FileHash -LiteralPath "$BackupDirectory\db_store_lts_instalacion_inicial.sql" -Algorithm SHA256
```

**`db_store_lts_instalacion_inicial.sql` es el dump final**, con estructura, datos iniciales, catálogos, rutinas, triggers y eventos deshabilitados. El script `02` es la herramienta para producirlo, no reemplaza ese dump. Guárdalo fuera del repositorio junto con su hash y la versión del backend compatible.

Prueba este dump en una tercera base vacía de verificación antes de distribuirlo: repite la creación/importación con ese nombre y ejecuta `03`. Si las vistas/rutinas contienen nombres de esquema explícitos, ajusta esas definiciones para la base de verificación; evita que sigan consultando `db_store_lts`. No ejecutes `02` allí: ya estás importando la plantilla limpia.

Para una instalación nueva, crea una base vacía e importa el dump final, configura los `DEFINER`/permisos, copia los archivos físicos conservados y conecta el backend a ella. Inicia sesión con `ROOT` y completa compañía, tienda y periodo si corresponde. Los cinco talonarios y el producto delivery ya vienen cargados; revisa las series y configura las credenciales e integraciones. No hace falta ejecutar de nuevo la limpieza.

## Alternativa desde DBeaver u otro editor

1. En la conexión de la base actual, usa **Backup/Exportación nativa** con estructura, todos los datos, procedimientos/funciones, triggers y eventos. La exportación simple de datos que genera solo `INSERT` no sirve como copia completa.
2. Revisa el dump según el paso 2 y deshabilita sus eventos antes de importarlo.
3. Ejecuta `01`, crea/abre una conexión cuyo esquema activo sea `db_store_lts` y restaura el dump completo allí.
4. Comprueba objetos y cuentas. Ejecuta `02` completo en esa conexión con autocommit, deteniéndote al primer error.
5. Ejecuta `03` y revisa los reportes.
6. Haz un nuevo **Backup/Exportación nativa** de `db_store_lts`, con las mismas opciones completas, llamado `db_store_lts_instalacion_inicial.sql`.

## Validación realizada

La comprobación del script se hace con una instancia local aislada y datos ficticios derivados de las tablas del repositorio, sin conectar al servidor de trabajo. Consulta `VALIDACION.md` para conocer los escenarios y la versión efectivamente probada. Valida también el dump de tu copia real y la versión de MySQL del servidor de destino antes de distribuirlo.
