-- Ejecutar despues de exportar la base original completa.
-- Si ya existe, falla deliberadamente para no reutilizar una copia con datos.
-- No selecciona ni modifica la base original.
CREATE DATABASE `db_store_lts`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
