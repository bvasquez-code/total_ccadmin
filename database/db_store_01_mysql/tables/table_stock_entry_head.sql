DROP PROCEDURE IF EXISTS `p_manage_stock_entry_head`;

DELIMITER $$

CREATE PROCEDURE `p_manage_stock_entry_head`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    -- 1. Verificamos si la tabla existe
    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'stock_entry_head';

    IF v_table_exists = 0 THEN
        -- =============================================
        -- CASO: LA TABLA NO EXISTE -> CREARLA COMPLETA
        -- =============================================
        CREATE TABLE `stock_entry_head` (
          `StockEntryCod` varchar(16) NOT NULL COMMENT 'PK. Codigo interno de la entrada excepcional de stock',
          `StoreCod` varchar(4) NOT NULL COMMENT 'Codigo de la tienda donde se realiza el movimiento',
          `ProcessType` char(1) NOT NULL COMMENT 'Tipo de proceso: O=Operacion original, R=Resolucion',
          `MovementMode` char(1) DEFAULT NULL COMMENT 'Modalidad: D=Directo, N=Pasa por no disponible. Nulo en resoluciones',
          `ReasonCode` varchar(64) DEFAULT NULL COMMENT 'ConfigCod del motivo de entrada. business_config GroupId=8',
          `OriginStockEntryCod` varchar(16) DEFAULT NULL COMMENT 'Codigo de la entrada original. Obligatorio solo para ProcessType=R',
          `ProcessStatus` char(1) NOT NULL DEFAULT 'P' COMMENT 'Estado del proceso: P=Pendiente, C=Confirmado, R=Rechazado, X=Anulado',
          `Observation` varchar(512) DEFAULT NULL COMMENT 'Observacion general del movimiento',
          `ConfirmUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que confirmo el movimiento',
          `ConfirmDate` datetime DEFAULT NULL COMMENT 'Fecha y hora de confirmacion',
          `ResolutionUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que confirmo la resolucion',
          `ResolutionDate` datetime DEFAULT NULL COMMENT 'Fecha y hora de resolucion',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que modifico por ultima vez',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico: A=Activo, I=Inactivo',
          PRIMARY KEY (`StockEntryCod`),
          KEY `idx_stock_entry_head_store_status` (`StoreCod`,`ProcessStatus`,`CreationDate`),
          KEY `idx_stock_entry_head_origin` (`OriginStockEntryCod`),
          KEY `idx_stock_entry_head_reason` (`ReasonCode`),
          CONSTRAINT `fk_stock_entry_head_store` FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`),
          CONSTRAINT `fk_stock_entry_head_reason` FOREIGN KEY (`ReasonCode`) REFERENCES `business_config` (`ConfigCod`),
          CONSTRAINT `fk_stock_entry_head_origin` FOREIGN KEY (`OriginStockEntryCod`) REFERENCES `stock_entry_head` (`StockEntryCod`),
          CONSTRAINT `chk_stock_entry_head_process_type` CHECK (`ProcessType` in (_utf8mb4'O',_utf8mb4'R')),
          CONSTRAINT `chk_stock_entry_head_movement_mode` CHECK (
              (`ProcessType` = _utf8mb4'O' AND `MovementMode` in (_utf8mb4'D',_utf8mb4'N')
                  AND `ReasonCode` IS NOT NULL AND `OriginStockEntryCod` IS NULL)
              OR
              (`ProcessType` = _utf8mb4'R' AND `MovementMode` IS NULL
                  AND `ReasonCode` IS NULL AND `OriginStockEntryCod` IS NOT NULL)
          ),
          CONSTRAINT `chk_stock_entry_head_process_status` CHECK (`ProcessStatus` in (_utf8mb4'P',_utf8mb4'C',_utf8mb4'R',_utf8mb4'X')),
          CONSTRAINT `chk_stock_entry_head_status` CHECK (`Status` in (_utf8mb4'A',_utf8mb4'I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Cabecera de entradas excepcionales de stock y sus resoluciones';

        SELECT 'Tabla stock_entry_head creada desde cero.' AS Mensaje;
    ELSE
        -- =============================================
        -- CASO: LA TABLA YA EXISTE -> APLICAR ALTERS
        -- =============================================
        SELECT 'Tabla stock_entry_head ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

-- Ejecutar y limpiar
CALL `p_manage_stock_entry_head`();
DROP PROCEDURE `p_manage_stock_entry_head`;
