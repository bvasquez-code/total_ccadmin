# Validación del script de inicialización

Fecha: 2026-10-06. Servidor usado: **MySQL 9.5.0**, en una instancia temporal independiente, ligada a `127.0.0.1:33316`, con un directorio de datos temporal propio. No se conectó a la base de trabajo ni se usaron sus credenciales.

Se crearon las 113 tablas a partir de los `CREATE TABLE` del repositorio, junto con los procedimientos de correlativos, automatizaciones de tienda/usuario y triggers existentes. Se insertaron datos ficticios: tres usuarios, personas, perfiles y permisos, compañía/tiendas anteriores, periodo histórico, cajas, productos, marcas, categorías, clientes, proveedores, compras, ventas, sesiones, talonarios, archivos y configuración de Mercado Pago/delivery. Otra base contenía un registro de control para comprobar que permaneciera intacta.

La versión corregida se probó con `D0000001`, dos variantes, un código de barras, precio/configuración propios de `T001`, impuestos con ID 12, una imagen, marca y una cadena de tres categorías. Los medios de pago incluyeron imágenes referenciadas por código y por ruta, además de un registro sin imagen. Se compararon todos los campos de las filas protegidas, **incluidas las fechas de auditoría**, antes y después de limpiar.

Después se registró `worldcities` con la estructura suministrada por el usuario. El manifiesto actual contiene 114 tablas: 69 para vaciar, 20 para conservar, 16 para reinicializar y 9 para limpieza selectiva. Se probó tanto una instalación sin este catálogo opcional como una con tres filas, incluidos valores nulos e identificadores repetidos permitidos por su estructura original. Dos limpiezas completas conservaron exactamente estructura y datos de `worldcities`, `city`, `country`, `state` y los tres catálogos de ubigeo, incluidas sus fechas de auditoría. El archivo `table_worldcities.sql` se probó creando la tabla y repitiendo su ejecución; ante una tabla existente no modifica estructura ni datos. Las 11 columnas conservan tipos y nulabilidad originales y tienen comentarios. Una tabla desconocida continúa cancelando antes de borrar datos.

| Escenario | Resultado |
| --- | --- |
| Correspondencia entre el manifiesto SQL y archivos de tablas | 114 tablas, sin omisiones ni duplicados: 69 para vaciar, 20 para conservar, 16 para reinicializar, 9 para limpieza selectiva. |
| Catálogos geográficos, incluido `worldcities` | Limpieza correcta con 114 tablas cuando está presente y con 113 cuando falta. Las siete tablas geográficas presentes permanecen idénticas después de dos limpiezas. Registro de `worldcities` idempotente y compatible con la definición original. |
| Limpieza completa | `Result=OK`, validación FK activa, 113 tablas revisadas y 213 FK sin huérfanos en la estructura ficticia. |
| Repetir una limpieza completada | Mismo contenido funcional de todas las tablas; se excluyeron las fechas de auditoría de la comparación. |
| Datos operativos y terceros | Eliminados los productos ordinarios y transacciones ficticias; solo permanece delivery y sus dependencias. |
| `payment_method` | Todas las columnas y filas idénticas antes/después, incluidos códigos/rutas de imágenes, valores vacíos y auditoría. |
| Imágenes | Archivos de medios de pago por código y por ruta, y la imagen delivery, conservados con todos sus campos. Archivos de productos eliminados y archivos sin referencias protegidas eliminados. |
| `D0000001` | Producto, variantes, código de barras, imagen, configuración de T001, impuestos, marca y tres categorías idénticos antes/después. Configuraciones de tiendas eliminadas retiradas. |
| Marca y categorías `GENERICO` | Probado con marca activa/inactiva, padre identificado por bandera o ausencia de padre, hijos activos/inactivos, nietos y un ancestro necesario. Todos sus campos, incluida auditoría, conservados exactamente en dos ejecuciones. Otras marcas/categorías y un hijo homónimo de otra rama se eliminan. Delivery, imágenes, medios de pago y configuración permanecen protegidos; 113 tablas y 213 FK comprobadas. |
| Nueva tienda | El trigger/procedimiento existente crea configuración, búsqueda y stock de delivery para una nueva T002, conservando precio y condición digital del producto base. |
| Talonarios | Los cinco registros y cinco asignaciones coinciden campo por campo con `counterfoil_202610061158.sql` y `counterfoil_store_202610061158.sql`, incluidas fechas. Todos los correlativos en cero. |
| Usuarios | Solo ROOT y USER_WEB; contraseñas conservadas, recuperación y `PasswordDecoded` vacíos. |
| Compañía, tienda, periodo, almacén y cajas | Estado inicial esperado; automatizaciones existentes crean almacén y cajas. |
| AUTO_INCREMENT | 1 para las tablas vaciadas con identidad; siguiente periodo en 2; siguiente impuesto delivery en 13, sin cambiar su ID 12 original. |
| Siguientes códigos | Se conserva el mínimo seguro de los registros existentes. Probado también con prefijo global D: el siguiente código es `D0000002`, sin reutilizar delivery. |
| Secuencias globales de 20 caracteres | Reproducido el rechazo anterior con las ocho definiciones de la captura del usuario. La validación corregida admite `LT`/`ES` + 18 dígitos y conserva prefijo, longitud y `UsePrefix` en dos limpiezas. `product_traceability` y `sunat_submission` quedan vacías con secuencia cero; los siguientes códigos son `LT000000000000000001` y `ES000000000000000001`. Delivery y `worldcities` permanecen intactos. Formatos con más de 18 dígitos numéricos o sin espacio para el número continúan cancelando antes de limpiar. |
| Ocho secuencias de tienda | Reproducido el rechazo anterior con las ocho definiciones y formatos de la captura. Dos limpiezas mantienen exactamente esos tipos, prefijos y longitudes para T001/periodo 1, con correlativos cero, sin crear `transfer_request_head`. El siguiente código de transferencia es `TT00100000000001`. Probado también con una definición adicional de `transfer_request_head`: se conserva su formato y reinicia su correlativo. Si falta `sale_head`, cancela antes de borrar e informa el nombre de esa definición. Delivery y `worldcities` permanecen intactos. |
| `business_config` | Todas las filas ajenas a Mercado Pago idénticas, incluidos tarifas, horarios y auditoría. En Mercado Pago solo cambia `Str3Config` (token privado); clave pública, moneda, idioma, cuotas, estado, modo y demás campos quedan idénticos. |
| Otra base seleccionada | Rechazo antes de modificar datos. |
| Tabla no clasificada | Rechazo antes de modificar datos. |
| USER_WEB inactivo o ROOT sin perfil activo | Rechazo antes de modificar datos. |
| Secuencia global con destino desconocido | Rechazo antes de modificar datos. |
| FK desde otro esquema | Rechazo; referencia externa conservada. |
| Tabla usada por la automatización ausente | Rechazo antes de modificar datos. |
| Tabla operativa opcional no instalada | Limpieza correcta de las 112 restantes; probado sin `mercado_pago_attempt`. |
| Evento habilitado | Rechazo antes de modificar datos. |
| Delivery ausente/inactivo | Rechazo antes de modificar datos. No se inventan datos del producto. |
| Archivo referenciado por un medio de pago inexistente | Rechazo antes de modificar datos. Referencias vacías permitidas y conservadas. |
| FK huérfana en permisos conservados | Error explícito en la comprobación de relaciones; no se acepta como plantilla válida. |
| Error provocado después de TRUNCATE | El manejador restauró `FOREIGN_KEY_CHECKS=1` y el valor previo de `group_concat_max_len`. Se restauró el fixture desde su respaldo, porque TRUNCATE no admite rollback. |
| Error provocado durante DELETE preparado | El manejador desasignó la sentencia preparada y restauró `FOREIGN_KEY_CHECKS=1` y `group_concat_max_len`. Se restauró el fixture desde su respaldo. |
| Exportación e importación completas | Dump ficticio con rutinas/triggers/eventos importado en una tercera base; consultas de `03` correctas y procedimiento auxiliar ausente. Delivery, imágenes, medios de pago y talonarios conservados en la importación. |
| Comandos PowerShell de la guía | Limpieza y consultas de verificación ejecutadas mediante `source`; se detectó y habilitó `--commands=ON` en el cliente 9.5. Los errores de un archivo ejecutado con `--execute=source ...` detuvieron el cliente con código distinto de cero. |
| Base de control | Registro intacto después de todos los escenarios. |

También se ejecutó `git diff --check` y se verificó el espacio en blanco de los archivos nuevos.

El número de FK corresponde a las definiciones probadas, no a una inspección de tu servidor. La base real puede tener migraciones adicionales o valores de configuración personalizados. **No se ha probado con un servidor MySQL 8 ni con un dump completo real**, porque los adjuntos contienen solo INSERT de compañía, tienda y talonarios. El script utiliza construcciones de MySQL 8; queda pendiente verificar el respaldo real y su importación en la versión de destino siguiendo el README.
