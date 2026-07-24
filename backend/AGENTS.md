# Instrucciones Backend

## Política documental

No leer automáticamente:

- `GUIA_ARQUITECTURA_BACKEND.md`
- `ws_wa_store_ccadmin/GUIA_ARQUITECTURA_BACKEND_JAVA.md`

Consultarlas únicamente cuando el usuario lo solicite según la política del
`AGENTS.md` raíz.

## Reglas obligatorias

- Mantener organización por dominio bajo `com.ccadmin.app`.
- No crear una arquitectura paralela.
- Separar escritura y comandos en `*CreateService`.
- Separar consultas, bandejas y formularios en `*SearchService`.
- Reservar `*TaskService` para tareas asíncronas o encoladas.
- Mantener controllers delgados: traducen HTTP y delegan.
- No consultar repositories directamente desde controllers.
- Antes de implementar una operación, buscar la lógica de negocio equivalente.
- No duplicar confirmaciones, Kardex, stock, precios, auditoría, estados,
  correlativos ni generación de búsquedas.
- Los procesos masivos reconstruyen DTOs y delegan en servicios especializados.
- Si el flujo normal y el masivo hacen lo mismo, ambos llaman al mismo núcleo.
- Preferir métodos de lista y `saveAll(...)` cuando la operación es colectiva.
- Los métodos públicos de escritura deben ser orquestadores legibles.
- Usar `@Transactional` para operaciones que modifican varias tablas o estados.
- Los procesos en segundo plano reciben `userCod` explícitamente y no dependen
  del `SecurityContext`.
- Respetar `AuditTableEntity` y sus métodos de sesión.
- Solicitar correlativos con autocommit antes de la transacción de negocio.
- Usar nombres descriptivos completos:
  `stockEntryHeadRepository`, no `headRepository`.
- Mantener `ResponseWsDto` y los contratos HTTP existentes.
- Agregar pruebas para núcleos reutilizados y ejecutar las pruebas Maven.

## Verificación

- Buscar referencias residuales después de renombrar o dividir clases.
- Compilar con Java 21.
- Ejecutar pruebas Maven.
- Ejecutar `git diff --check`.
