DROP PROCEDURE IF EXISTS `p_manage_bulk_load_destination`;

DELIMITER $$

CREATE PROCEDURE `p_manage_bulk_load_destination`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'bulk_load_destination'
    ) THEN
        CREATE TABLE `bulk_load_destination` (
          `BulkLoadCod` varchar(16) NOT NULL COMMENT 'PK y FK. Codigo de la carga masiva',
          `StoreCod` varchar(4) NOT NULL COMMENT 'PK y FK. Local de destino congelado durante la validacion',
          `ProcessStatus` char(1) NOT NULL DEFAULT 'P'
              COMMENT 'P=Pendiente, Q=En cola, W=Procesando, F=Finalizado, E=Error, X=Anulado',
          `NumTotalDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad total de detalles generados para el local',
          `NumProcessedDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles del local que terminaron con exito o error',
          `NumSuccessDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles confirmados correctamente para el local',
          `NumErrorDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles con error para el local',
          `StartDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que inicio el procesamiento del local',
          `EndDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que finalizo o fallo el procesamiento del local',
          `StatusMessage` varchar(512) DEFAULT NULL COMMENT 'Resumen legible del estado del procesamiento del local',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que registro el destino',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion del destino',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que realizo la ultima modificacion',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico del registro: A=Activo, I=Inactivo',
          PRIMARY KEY (`BulkLoadCod`,`StoreCod`),
          KEY `idx_bulk_load_destination_store` (`StoreCod`,`ProcessStatus`),
          CONSTRAINT `fk_bulk_load_destination_head`
              FOREIGN KEY (`BulkLoadCod`) REFERENCES `bulk_load_head` (`BulkLoadCod`),
          CONSTRAINT `fk_bulk_load_destination_store`
              FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`),
          CONSTRAINT `chk_bulk_load_destination_process_status`
              CHECK (`ProcessStatus` in (_utf8mb4'P',_utf8mb4'Q',_utf8mb4'W',_utf8mb4'F',_utf8mb4'E',_utf8mb4'X')),
          CONSTRAINT `chk_bulk_load_destination_counters`
              CHECK (
                  `NumTotalDetails` >= 0
                  AND `NumProcessedDetails` >= 0
                  AND `NumSuccessDetails` >= 0
                  AND `NumErrorDetails` >= 0
              ),
          CONSTRAINT `chk_bulk_load_destination_status`
              CHECK (`Status` in (_utf8mb4'A',_utf8mb4'I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Locales resueltos y congelados al validar una carga masiva';
    ELSE
        ALTER TABLE `bulk_load_destination`
            MODIFY COLUMN `BulkLoadCod` varchar(16) NOT NULL COMMENT 'PK y FK. Codigo de la carga masiva',
            MODIFY COLUMN `StoreCod` varchar(4) NOT NULL COMMENT 'PK y FK. Local de destino congelado durante la validacion',
            MODIFY COLUMN `ProcessStatus` char(1) NOT NULL DEFAULT 'P' COMMENT 'P=Pendiente, Q=En cola, W=Procesando, F=Finalizado, E=Error, X=Anulado',
            MODIFY COLUMN `NumTotalDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad total de detalles generados para el local',
            MODIFY COLUMN `NumProcessedDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles del local que terminaron con exito o error',
            MODIFY COLUMN `NumSuccessDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles confirmados correctamente para el local',
            MODIFY COLUMN `NumErrorDetails` int NOT NULL DEFAULT 0 COMMENT 'Cantidad de detalles con error para el local',
            MODIFY COLUMN `StartDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que inicio el procesamiento del local',
            MODIFY COLUMN `EndDate` datetime DEFAULT NULL COMMENT 'Fecha y hora en que finalizo o fallo el procesamiento del local',
            MODIFY COLUMN `StatusMessage` varchar(512) DEFAULT NULL COMMENT 'Resumen legible del estado del procesamiento del local',
            MODIFY COLUMN `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que registro el destino',
            MODIFY COLUMN `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion del destino',
            MODIFY COLUMN `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que realizo la ultima modificacion',
            MODIFY COLUMN `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion',
            MODIFY COLUMN `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico del registro: A=Activo, I=Inactivo';
    END IF;
END $$

DELIMITER ;

CALL `p_manage_bulk_load_destination`();
DROP PROCEDURE `p_manage_bulk_load_destination`;
