# Instrucciones del proyecto

## Alcance

Estas instrucciones aplican a todo el repositorio.

Existen instrucciones adicionales por ámbito:

- `backend/AGENTS.md`
- `frontend/AGENTS.md`
- `database/AGENTS.md`

## Política de lectura de guías

El proyecto contiene guías resumidas y guías extensas.

No abrir ni leer automáticamente ninguna guía de arquitectura.

Solo consultar una guía cuando el usuario:

1. solicite expresamente leer o consultar las guías;
2. mencione o enlace una guía concreta;
3. pida verificar el cumplimiento de las convenciones documentadas;
4. indique explícitamente que se aplique la guía de backend, frontend o database.

Una tarea ordinaria de implementación, corrección, análisis o revisión no
autoriza por sí misma la lectura de las guías.

Cuando el usuario autorice una consulta genérica:

1. leer primero la guía resumida del ámbito;
2. consultar la guía extensa solamente si el usuario la menciona o si la
   autorización abarca las guías y el resumen no resuelve la cuestión.

Si no existe autorización, aplicar únicamente las reglas contenidas en los
`AGENTS.md` y los patrones visibles en el código involucrado.

## Reglas transversales obligatorias

- Preservar cambios del usuario y evitar modificaciones fuera del alcance.
- Antes de crear una lógica nueva, inspeccionar el flujo equivalente existente.
- Centralizar la lógica de negocio; no duplicarla para procesos masivos,
  asíncronos o especializados.
- Un flujo especializado debe transformar su entrada y delegar en el servicio
  de dominio correspondiente.
- Extraer un núcleo común cuando dos entradas ejecuten la misma confirmación,
  persistencia, auditoría o efecto de negocio.
- Usar nombres completos y descriptivos para dependencias y colaboradores.
- Mantener compatibilidad con los contratos existentes salvo solicitud expresa.
- Verificar los cambios en proporción al riesgo y ejecutar `git diff --check`.
- No modificar ni eliminar las guías extensas al actualizar sus resúmenes.

## Fuentes disponibles bajo demanda

Resúmenes:

- `backend/GUIA_ARQUITECTURA_BACKEND.md`
- `frontend/GUIA_ARQUITECTURA_FRONTEND.md`
- `database/GUIA_ARQUITECTURA_DATABASE.md`

Referencias extensas:

- `backend/ws_wa_store_ccadmin/GUIA_ARQUITECTURA_BACKEND_JAVA.md`
- `frontend/web_ccadmin_lt/docs/guia-arquitectura-frontend-angular.md`
- `database/db_store_01_mysql/GUIA_ARQUITECTURA_CONVENCIONES_DB.md`
