# Instrucciones Database

## Política documental

No leer automáticamente:

- `GUIA_ARQUITECTURA_DATABASE.md`
- `db_store_01_mysql/GUIA_ARQUITECTURA_CONVENCIONES_DB.md`

Consultarlas únicamente cuando el usuario lo solicite según la política del
`AGENTS.md` raíz.

## Reglas obligatorias

- Mantener compatibilidad con MySQL 8.
- Crear tablas en `db_store_01_mysql/tables/table_<table>.sql`.
- Usar tablas en `snake_case` y columnas funcionales compatibles con Java.
- Encapsular mantenimiento en `p_manage_<table>`.
- Hacer scripts rerunnable e idempotentes mediante `information_schema`.
- No usar `DROP TABLE` como mecanismo normal de actualización.
- Incluir auditoría común salvo excepción explícitamente justificada.
- Toda columna nueva debe tener `COMMENT`.
- Los bloques `ALTER` deben preservar o agregar los comentarios.
- Documentar el significado de estados, tipos, unidades y precisiones.
- Usar letras para estados cerrados.
- Usar códigos descriptivos para tipos o catálogos extensibles.
- Respetar PK, FK e índices y revisar dependencias antes de alterarlos.
- Nombrar FK con `fk_`, índices funcionales con `idx_` y únicos con `uk_` o
  `uq_`.
- Conservar `get_cod_seq` y `get_cod_trx`.
- Solicitar correlativos con autocommit antes de la transacción de negocio.
- Aceptar huecos de correlativo; nunca permitir códigos repetidos.
- Ubicar triggers en `trigger` y nombrarlos con prefijo `trg_`.
- Usar inserts de configuración idempotentes.
- No eliminar ni reescribir guías existentes.

## Verificación

- Comprobar comentarios de todas las columnas.
- Revisar idempotencia, PK, FK, índices y orden de ejecución.
- Validar en MySQL cuando exista un entorno disponible.
- Ejecutar `git diff --check`.
