# Guía de arquitectura Database

## Propósito

Esta es la versión operativa y resumida de las convenciones de base de datos.

La referencia extensa se mantiene en:

- `db_store_01_mysql/GUIA_ARQUITECTURA_CONVENCIONES_DB.md`

## Plataforma y estructura

- MySQL 8.
- Motor InnoDB.
- Charset `utf8mb4`.
- Collation `utf8mb4_0900_ai_ci`.
- Tablas en `db_store_01_mysql/tables`.
- Procedures y functions persistentes en `db_store_01_mysql/procedures`.
- Triggers en `db_store_01_mysql/trigger`.

No usar sintaxis Oracle, packages, `SYS_REFCURSOR`, secuencias nativas ni
`RAISE_APPLICATION_ERROR`.

## Nombres

- Tabla: `snake_case`.
- Archivo de tabla: `table_<table_name>.sql`.
- Cabecera: sufijo `_head`.
- Detalle: sufijo `_det`.
- Columna funcional: PascalCase compatible con las entities Java.
- Código: sufijo `Cod`.
- Cantidad o importe: prefijo `Num`.
- Indicador: prefijo `Is` o `Has`.
- FK: `fk_<table>_<reference>`.
- Índice funcional: `idx_<table>_<purpose>`.
- Único: `uk_` o `uq_`.
- Trigger: `trg_<event>_<table>`.
- Procedure temporal: `p_manage_<table>`.

Los estados cerrados pueden almacenarse como `char(1)` con letras documentadas.
Los tipos o catálogos que pueden crecer deben usar códigos descriptivos con
longitud suficiente; no deben limitarse al abecedario.

## Campos y documentación

Toda columna nueva debe incluir `COMMENT`, sin excepciones implícitas.

El comentario debe explicar:

- propósito;
- significado de letras o códigos;
- unidad o precisión cuando aplique;
- carácter de PK/FK si aporta claridad;
- si el valor es informativo, calculado o extensible.

Si una tabla ya existe, los `ALTER TABLE ... MODIFY COLUMN` del bloque de
mantenimiento también deben conservar o agregar los comentarios.

## Auditoría

Incluir normalmente:

```sql
`CreationUser` varchar(16) NOT NULL,
`CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`ModifyUser` varchar(16) DEFAULT NULL,
`ModifyDate` datetime NOT NULL
    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`Status` char(1) NOT NULL DEFAULT 'A'
```

Documentar `A=Activo` e `I=Inactivo`.

Una tabla histórica inmutable puede justificar solo los datos de creación, pero
la decisión debe quedar expresada en sus comentarios.

## Scripts de tabla

Los scripts deben ser rerunnable e idempotentes:

```sql
DROP PROCEDURE IF EXISTS `p_manage_example`;

DELIMITER $$

CREATE PROCEDURE `p_manage_example`()
BEGIN
    IF NOT EXISTS (...) THEN
        CREATE TABLE ...;
    ELSE
        -- ALTER idempotentes verificados en information_schema
    END IF;
END $$

DELIMITER ;

CALL `p_manage_example`();
DROP PROCEDURE `p_manage_example`;
```

Reglas:

1. consultar `DATABASE()` e `information_schema`;
2. crear la tabla completa si no existe;
3. aplicar cambios solamente cuando sean necesarios;
4. no usar `DROP TABLE` como actualización normal;
5. preservar datos existentes;
6. emitir mensajes útiles cuando el patrón del script los contemple;
7. revisar compatibilidad y dependencias antes de alterar PK o FK.

## Claves, relaciones e índices

- Maestro: preferir código funcional como PK cuando el dominio lo use.
- Cabecera: `<Entity>Cod`.
- Detalle repetible: código de cabecera + `ItemNumber`.
- Tabla puente: PK compuesta por los códigos relacionados.
- Crear FK físicas cuando el modelo existente las use.
- Crear índices de soporte para FK y consultas frecuentes.
- Antes de cambiar una PK, inspeccionar su forma actual.
- Antes de eliminar una PK usada por una FK, crear el índice secundario que
  preserve la relación.
- Evitar cascadas nuevas sin analizar el comportamiento del dominio.

## Correlativos

- `get_cod_seq`: código global mediante `table_sequence`.
- `get_cod_trx`: código por tienda y período mediante `store_sequence`.
- Ambos administran autocommit.
- Solicitar el código antes de iniciar una transacción de negocio que no deba
  confirmarse anticipadamente.
- Se aceptan huecos de correlativo; no se acepta repetir códigos.
- No reemplazar estos mecanismos sin una decisión explícita.

## Procedures y triggers

- Parámetros de procedure: prefijo `p_`.
- Variables locales: `v_` o `l_`.
- Usar handlers MySQL y `RESIGNAL` cuando aplique.
- Los triggers deben ser pequeños y delegar cuando la complejidad lo justifique.
- Documentar efectos automáticos, especialmente históricos y sincronizaciones.
- Un histórico debe registrar únicamente cambios efectivos cuando esa sea la
  regla del negocio.

## Datos de configuración

- Preferir inserts idempotentes.
- Usar `INSERT ... SELECT ... WHERE NOT EXISTS` u
  `ON DUPLICATE KEY UPDATE` según la clave y el efecto esperado.
- No duplicar catálogos ni permisos al reejecutar un instalador.
- Documentar dependencias y orden de ejecución.

## Cambios riesgosos

- No asumir que existe rollback automático.
- Preparar un script de reversión explícito cuando el cambio sea difícil de
  recuperar.
- No ejecutar scripts destructivos sin resolver exactamente el objetivo.
- Revisar primero FK, índices, datos y compatibilidad MySQL.

## Verificación mínima

Antes de entregar cambios DB:

1. comprobar que todas las columnas nuevas tengan comentario;
2. verificar idempotencia de creación y actualización;
3. revisar PK, FK e índices;
4. revisar orden de ejecución;
5. comprobar códigos de estado y tipos;
6. validar el comportamiento de correlativos y autocommit;
7. ejecutar el script contra MySQL cuando exista un entorno disponible;
8. ejecutar `git diff --check`.
