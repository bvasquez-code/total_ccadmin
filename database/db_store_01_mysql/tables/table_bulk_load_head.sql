DROP PROCEDURE IF EXISTS `p_manage_bulk_load_head`;

DELIMITER $$

CREATE PROCEDURE `p_manage_bulk_load_head`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'bulk_load_head'
    ) THEN
        CREATE TABLE `bulk_load_head` (
          `BulkLoadCod` varchar(16) NOT NULL COMMENT 'PK. Codigo unico de la carga masiva',
          `BulkLoadType` varchar(64) NOT NULL
              COMMENT 'Codigo descriptivo y extensible del tipo de carga, por ejemplo PRODUCT_PRICE o STOCK_ENTRY',
          `SchemaVersion` int NOT NULL DEFAULT 1 COMMENT 'Version del esquema de datos interpretado por el frontend',
          `ProcessStatus` char(1) NOT NULL DEFAULT 'D'
              COMMENT 'D=Borrador, V=Validando, P=Pendiente, Q=En cola, W=Procesando, F=Finalizado, E=Error, X=Anulado',
          `SourceFileCod` varchar(20) DEFAULT NULL COMMENT 'FK opcional al archivo fuente conservado en app_file',
          `ErrorFileCod` varchar(20) DEFAULT NULL COMMENT 'FK opcional al archivo de errores conservado en app_file',
          `OriginalFileName` varchar(255) DEFAULT NULL COMMENT 'Nombre original del archivo seleccionado por el usuario',
          `FileHash` char(64) DEFAULT NULL COMMENT 'Hash SHA-256 opcional del archivo fuente para trazabilidad',
          `Parameters` json DEFAULT NULL COMMENT 'Parametros y metadatos extensibles usados por el tipo de carga',
          `NumSourceRows` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de filas de negocio leidas desde el archivo',
          `NumDestinations` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de locales resueltos al validar la carga',
          `NumTotalDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad total de detalles generados despues de expandir por local',
          `NumProcessedDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles que terminaron con exito o error',
          `NumSuccessDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles confirmados correctamente',
          `NumErrorDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles con error de validacion o procesamiento',
          `NumWarningDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles que contienen al menos una advertencia',
          `ProgressPercent` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT 'Porcentaje persistido de avance entre 0.00 y 100.00',
          `ValidationDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que termino la validacion del contenido',
          `QueueDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que la carga fue enviada a la cola',
          `StartDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que inicio el procesamiento en segundo plano',
          `EndDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que finalizo o fallo definitivamente el proceso',
          `LastHeartbeatDate` datetime DEFAULT NULL COMMENT 'Ultima actividad registrada por el trabajador en segundo plano',
          `StatusMessage` varchar(512) DEFAULT NULL COMMENT 'Resumen legible del estado o ultimo resultado del proceso',
          `AttemptCount` int NOT NULL DEFAULT 0 COMMENT 'Numero de veces que el trabajador intento ejecutar la carga',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que registro la carga masiva',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que realizo la ultima modificacion',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico del registro: A=Activo, I=Inactivo',
          PRIMARY KEY (`BulkLoadCod`),
          KEY `idx_bulk_load_head_tray` (`Status`,`CreationDate`,`ProcessStatus`),
          KEY `idx_bulk_load_head_queue` (`ProcessStatus`,`LastHeartbeatDate`),
          KEY `idx_bulk_load_head_type` (`BulkLoadType`,`CreationDate`),
          KEY `idx_bulk_load_head_source_file` (`SourceFileCod`),
          KEY `idx_bulk_load_head_error_file` (`ErrorFileCod`),
          CONSTRAINT `fk_bulk_load_head_source_file`
              FOREIGN KEY (`SourceFileCod`) REFERENCES `app_file` (`FileCod`),
          CONSTRAINT `fk_bulk_load_head_error_file`
              FOREIGN KEY (`ErrorFileCod`) REFERENCES `app_file` (`FileCod`),
          CONSTRAINT `chk_bulk_load_head_process_status`
              CHECK (`ProcessStatus` in (_utf8mb4'D',_utf8mb4'V',_utf8mb4'P',_utf8mb4'Q',_utf8mb4'W',_utf8mb4'F',_utf8mb4'E',_utf8mb4'X')),
          CONSTRAINT `chk_bulk_load_head_counters`
              CHECK (
                  `NumSourceRows` >= 0
                  AND `NumDestinations` >= 0
                  AND `NumTotalDetails` >= 0
                  AND `NumProcessedDetails` >= 0
                  AND `NumSuccessDetails` >= 0
                  AND `NumErrorDetails` >= 0
                  AND `NumWarningDetails` >= 0
                  AND `ProgressPercent` >= 0
                  AND `ProgressPercent` <= 100
              ),
          CONSTRAINT `chk_bulk_load_head_status`
              CHECK (`Status` in (_utf8mb4'A',_utf8mb4'I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Cabecera generica de procesos de carga masiva';
    ELSE
        -- Las versiones iniciales usaban P/S. El tipo es un catalogo extensible,
        -- por lo que no debe limitarse a una letra ni a una lista cerrada.
        IF EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_schema = DATABASE()
              AND table_name = 'bulk_load_head'
              AND constraint_name = 'chk_bulk_load_head_type'
              AND constraint_type = 'CHECK'
        ) THEN
            ALTER TABLE `bulk_load_head`
                DROP CHECK `chk_bulk_load_head_type`;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'bulk_load_head'
              AND (
                  data_type <> 'varchar'
                  OR character_maximum_length < 64
              )
        ) THEN
            ALTER TABLE `bulk_load_head`
                MODIFY COLUMN `BulkLoadType` varchar(64) NOT NULL
                COMMENT 'Codigo descriptivo y extensible del tipo de carga, por ejemplo PRODUCT_PRICE o STOCK_ENTRY';
        END IF;

        UPDATE `bulk_load_head`
        SET `BulkLoadType` = CASE `BulkLoadType`
            WHEN 'P' THEN 'PRODUCT_PRICE'
            WHEN 'S' THEN 'STOCK_ENTRY'
            ELSE `BulkLoadType`
        END
        WHERE `BulkLoadType` IN ('P', 'S');

        ALTER TABLE `bulk_load_head`
            MODIFY COLUMN `BulkLoadCod` varchar(16) NOT NULL COMMENT 'PK. Codigo unico de la carga masiva',
            MODIFY COLUMN `BulkLoadType` varchar(64) NOT NULL COMMENT 'Codigo descriptivo y extensible del tipo de carga, por ejemplo PRODUCT_PRICE o STOCK_ENTRY',
            MODIFY COLUMN `SchemaVersion` int NOT NULL DEFAULT 1 COMMENT 'Version del esquema de datos interpretado por el frontend',
            MODIFY COLUMN `ProcessStatus` char(1) NOT NULL DEFAULT 'D' COMMENT 'D=Borrador, V=Validando, P=Pendiente, Q=En cola, W=Procesando, F=Finalizado, E=Error, X=Anulado',
            MODIFY COLUMN `SourceFileCod` varchar(20) DEFAULT NULL COMMENT 'FK opcional al archivo fuente conservado en app_file',
            MODIFY COLUMN `ErrorFileCod` varchar(20) DEFAULT NULL COMMENT 'FK opcional al archivo de errores conservado en app_file',
            MODIFY COLUMN `OriginalFileName` varchar(255) DEFAULT NULL COMMENT 'Nombre original del archivo seleccionado por el usuario',
            MODIFY COLUMN `FileHash` char(64) DEFAULT NULL COMMENT 'Hash SHA-256 opcional del archivo fuente para trazabilidad',
            MODIFY COLUMN `Parameters` json DEFAULT NULL COMMENT 'Parametros y metadatos extensibles usados por el tipo de carga',
            MODIFY COLUMN `NumSourceRows` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de filas de negocio leidas desde el archivo',
            MODIFY COLUMN `NumDestinations` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de locales resueltos al validar la carga',
            MODIFY COLUMN `NumTotalDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad total de detalles generados despues de expandir por local',
            MODIFY COLUMN `NumProcessedDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles que terminaron con exito o error',
            MODIFY COLUMN `NumSuccessDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles confirmados correctamente',
            MODIFY COLUMN `NumErrorDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles con error de validacion o procesamiento',
            MODIFY COLUMN `NumWarningDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles que contienen al menos una advertencia',
            MODIFY COLUMN `ProgressPercent` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT 'Porcentaje persistido de avance entre 0.00 y 100.00',
            MODIFY COLUMN `ValidationDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que termino la validacion del contenido',
            MODIFY COLUMN `QueueDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que la carga fue enviada a la cola',
            MODIFY COLUMN `StartDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que inicio el procesamiento en segundo plano',
            MODIFY COLUMN `EndDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que finalizo o fallo definitivamente el proceso',
            MODIFY COLUMN `LastHeartbeatDate` datetime DEFAULT NULL COMMENT 'Ultima actividad registrada por el trabajador en segundo plano',
            MODIFY COLUMN `StatusMessage` varchar(512) DEFAULT NULL COMMENT 'Resumen legible del estado o ultimo resultado del proceso',
            MODIFY COLUMN `AttemptCount` int NOT NULL DEFAULT 0 COMMENT 'Numero de veces que el trabajador intento ejecutar la carga',
            MODIFY COLUMN `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que registro la carga masiva',
            MODIFY COLUMN `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion del registro',
            MODIFY COLUMN `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que realizo la ultima modificacion',
            MODIFY COLUMN `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion',
            MODIFY COLUMN `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico del registro: A=Activo, I=Inactivo';
    END IF;
END $$

DELIMITER ;

CALL `p_manage_bulk_load_head`();
DROP PROCEDURE `p_manage_bulk_load_head`;
