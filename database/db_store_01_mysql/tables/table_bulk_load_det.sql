DROP PROCEDURE IF EXISTS `p_manage_bulk_load_det`;

DELIMITER $$

CREATE PROCEDURE `p_manage_bulk_load_det`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'bulk_load_det'
    ) THEN
        CREATE TABLE `bulk_load_det` (
          `BulkLoadCod` varchar(16) NOT NULL COMMENT 'PK y FK. Codigo de la carga masiva',
          `ItemNumber` int NOT NULL COMMENT 'PK. Correlativo del detalle dentro de la carga',
          `SourceRowNumber` int NOT NULL COMMENT 'Numero de fila original del Excel para trazabilidad',
          `StoreCod` varchar(4) DEFAULT NULL COMMENT 'FK. Local al que se aplica el detalle; puede ser nulo si el local no valido',
          `BusinessKey` varchar(128) DEFAULT NULL COMMENT 'Clave funcional legible para busqueda e identificacion del detalle',
          `Payload` json DEFAULT NULL COMMENT 'Datos normalizados y extensibles que procesa el tipo de carga',
          `ProcessStatus` char(1) NOT NULL DEFAULT 'P'
              COMMENT 'P=Pendiente, W=Procesando, C=Confirmado, E=Error, X=Anulado',
          `ErrorDetail` json DEFAULT NULL COMMENT 'Lista estructurada de errores de validacion o procesamiento',
          `WarningDetail` json DEFAULT NULL COMMENT 'Lista estructurada de advertencias que no bloquean el proceso',
          `ResultData` json DEFAULT NULL COMMENT 'Referencias y resultados generados por el proceso de negocio',
          `AttemptCount` int NOT NULL DEFAULT 0 COMMENT 'Numero de veces que se intento procesar el detalle',
          `StartDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que inicio el procesamiento del detalle',
          `EndDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que termino el procesamiento del detalle',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que registro el detalle',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion del detalle',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que realizo la ultima modificacion',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico del registro: A=Activo, I=Inactivo',
          PRIMARY KEY (`BulkLoadCod`,`ItemNumber`),
          KEY `idx_bulk_load_det_pending` (`BulkLoadCod`,`ProcessStatus`,`ItemNumber`),
          KEY `idx_bulk_load_det_store` (`BulkLoadCod`,`StoreCod`,`ProcessStatus`),
          KEY `idx_bulk_load_det_business` (`BusinessKey`),
          CONSTRAINT `fk_bulk_load_det_head`
              FOREIGN KEY (`BulkLoadCod`) REFERENCES `bulk_load_head` (`BulkLoadCod`),
          CONSTRAINT `fk_bulk_load_det_store`
              FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`),
          CONSTRAINT `chk_bulk_load_det_process_status`
              CHECK (`ProcessStatus` in (_utf8mb4'P',_utf8mb4'W',_utf8mb4'C',_utf8mb4'E',_utf8mb4'X')),
          CONSTRAINT `chk_bulk_load_det_item`
              CHECK (`ItemNumber` > 0 AND `SourceRowNumber` > 0 AND `AttemptCount` >= 0),
          CONSTRAINT `chk_bulk_load_det_status`
              CHECK (`Status` in (_utf8mb4'A',_utf8mb4'I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Detalle generico expandido por local de una carga masiva';
    ELSE
        ALTER TABLE `bulk_load_det`
            MODIFY COLUMN `BulkLoadCod` varchar(16) NOT NULL COMMENT 'PK y FK. Codigo de la carga masiva',
            MODIFY COLUMN `ItemNumber` int NOT NULL COMMENT 'PK. Correlativo del detalle dentro de la carga',
            MODIFY COLUMN `SourceRowNumber` int NOT NULL COMMENT 'Numero de fila original del Excel para trazabilidad',
            MODIFY COLUMN `StoreCod` varchar(4) DEFAULT NULL COMMENT 'FK. Local al que se aplica el detalle; puede ser nulo si el local no valido',
            MODIFY COLUMN `BusinessKey` varchar(128) DEFAULT NULL COMMENT 'Clave funcional legible para busqueda e identificacion del detalle',
            MODIFY COLUMN `Payload` json DEFAULT NULL COMMENT 'Datos normalizados y extensibles que procesa el tipo de carga',
            MODIFY COLUMN `ProcessStatus` char(1) NOT NULL DEFAULT 'P' COMMENT 'P=Pendiente, W=Procesando, C=Confirmado, E=Error, X=Anulado',
            MODIFY COLUMN `ErrorDetail` json DEFAULT NULL COMMENT 'Lista estructurada de errores de validacion o procesamiento',
            MODIFY COLUMN `WarningDetail` json DEFAULT NULL COMMENT 'Lista estructurada de advertencias que no bloquean el proceso',
            MODIFY COLUMN `ResultData` json DEFAULT NULL COMMENT 'Referencias y resultados generados por el proceso de negocio',
            MODIFY COLUMN `AttemptCount` int NOT NULL DEFAULT 0 COMMENT 'Numero de veces que se intento procesar el detalle',
            MODIFY COLUMN `StartDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que inicio el procesamiento del detalle',
            MODIFY COLUMN `EndDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que termino el procesamiento del detalle',
            MODIFY COLUMN `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que registro el detalle',
            MODIFY COLUMN `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion del detalle',
            MODIFY COLUMN `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que realizo la ultima modificacion',
            MODIFY COLUMN `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion',
            MODIFY COLUMN `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico del registro: A=Activo, I=Inactivo';
    END IF;
END $$

DELIMITER ;

CALL `p_manage_bulk_load_det`();
DROP PROCEDURE `p_manage_bulk_load_det`;
