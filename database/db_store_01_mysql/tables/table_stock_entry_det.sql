DROP PROCEDURE IF EXISTS `p_manage_stock_entry_det`;

DELIMITER $$

CREATE PROCEDURE `p_manage_stock_entry_det`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    -- 1. Verificamos si la tabla existe
    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'stock_entry_det';

    IF v_table_exists = 0 THEN
        -- =============================================
        -- CASO: LA TABLA NO EXISTE -> CREARLA COMPLETA
        -- =============================================
        CREATE TABLE `stock_entry_det` (
          `StockEntryCod` varchar(16) NOT NULL COMMENT 'Codigo de cabecera de entrada excepcional',
          `ItemNumber` int NOT NULL COMMENT 'Numero correlativo del item dentro del documento',
          `ProductCod` varchar(20) NOT NULL COMMENT 'Codigo del producto',
          `Variant` varchar(4) NOT NULL DEFAULT (_utf8mb4'0000') COMMENT 'Codigo de variante',
          `WarehouseCod` varchar(8) NOT NULL COMMENT 'Almacen afectado por el movimiento',
          `LotNumber` varchar(32) DEFAULT NULL COMMENT 'Numero de lote del producto, si aplica',
          `ExpirationDate` date DEFAULT NULL COMMENT 'Fecha de vencimiento del lote, si aplica',
          `ProductUnitName` varchar(32) NOT NULL DEFAULT 'NIU' COMMENT 'Unidad visible usada al registrar el detalle',
          `ProductUnitFactor` int NOT NULL DEFAULT '1' COMMENT 'Factor de conversion entre unidad visible y unidad interna',
          `NumUnit` int NOT NULL COMMENT 'Cantidad interna original que se pretende agregar',
          `NumUnitPending` int NOT NULL DEFAULT '0' COMMENT 'Cantidad interna que permanece no disponible',
          `NumUnitResolvedIn` int NOT NULL DEFAULT '0' COMMENT 'Cantidad interna resuelta que termina disponible',
          `NumUnitResolvedOut` int NOT NULL DEFAULT '0' COMMENT 'Cantidad interna resuelta que sale definitivamente',
          `UnavailableReasonCode` varchar(64) DEFAULT NULL COMMENT 'ConfigCod del motivo de no disponible. business_config GroupId=10',
          `ResolvedInReasonCode` varchar(64) DEFAULT NULL COMMENT 'Ultimo ConfigCod usado para regresar cantidad a disponible. business_config GroupId=11',
          `ResolvedOutReasonCode` varchar(64) DEFAULT NULL COMMENT 'Ultimo ConfigCod usado para retirar cantidad definitivamente. business_config GroupId=12',
          `ResolvedOutType` char(1) DEFAULT NULL COMMENT 'Tipo de salida definitiva: B=Baja, D=Destruccion',
          `ResolutionVersion` int NOT NULL DEFAULT '0' COMMENT 'Version incremental para identificar cada resolucion parcial en kardex',
          `OriginStockEntryCod` varchar(16) DEFAULT NULL COMMENT 'Codigo del documento original para una linea de resolucion',
          `OriginItemNumber` int DEFAULT NULL COMMENT 'Item original que se resuelve',
          `ResolutionType` char(1) DEFAULT NULL COMMENT 'Resolucion: L=Liberar, B=Baja definitiva, D=Destruir, M=Mantener no disponible',
          `ResolutionReasonCode` varchar(64) DEFAULT NULL COMMENT 'ConfigCod de resolucion. GroupId=11 para L; GroupId=12 para B o D',
          `Observation` varchar(512) DEFAULT NULL COMMENT 'Observacion particular de la linea o resolucion',
          `NextReviewDate` date DEFAULT NULL COMMENT 'Fecha de proxima revision cuando ResolutionType=M',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que modifico por ultima vez',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico: A=Activo, I=Inactivo',
          PRIMARY KEY (`StockEntryCod`,`ItemNumber`),
          KEY `idx_stock_entry_det_product` (`ProductCod`,`Variant`,`WarehouseCod`),
          KEY `idx_stock_entry_det_origin` (`OriginStockEntryCod`,`OriginItemNumber`),
          KEY `idx_stock_entry_det_unavailable_reason` (`UnavailableReasonCode`),
          KEY `idx_stock_entry_det_resolved_in_reason` (`ResolvedInReasonCode`),
          KEY `idx_stock_entry_det_resolved_out_reason` (`ResolvedOutReasonCode`),
          KEY `idx_stock_entry_det_resolution_reason` (`ResolutionReasonCode`),
          KEY `idx_stock_entry_det_warehouse` (`WarehouseCod`),
          CONSTRAINT `fk_stock_entry_det_head` FOREIGN KEY (`StockEntryCod`) REFERENCES `stock_entry_head` (`StockEntryCod`),
          CONSTRAINT `fk_stock_entry_det_variant` FOREIGN KEY (`ProductCod`,`Variant`) REFERENCES `product_variant` (`ProductCod`,`Variant`),
          CONSTRAINT `fk_stock_entry_det_warehouse` FOREIGN KEY (`WarehouseCod`) REFERENCES `warehouse` (`WarehouseCod`),
          CONSTRAINT `fk_stock_entry_det_origin` FOREIGN KEY (`OriginStockEntryCod`,`OriginItemNumber`)
              REFERENCES `stock_entry_det` (`StockEntryCod`,`ItemNumber`),
          CONSTRAINT `chk_stock_entry_det_quantity` CHECK (
              `NumUnit` > 0
              AND `NumUnitPending` >= 0
              AND `NumUnitResolvedIn` >= 0
              AND `NumUnitResolvedOut` >= 0
              AND (`NumUnitPending` + `NumUnitResolvedIn` + `NumUnitResolvedOut`) <= `NumUnit`
          ),
          CONSTRAINT `chk_stock_entry_det_factor` CHECK (`ProductUnitFactor` > 0),
          CONSTRAINT `chk_stock_entry_det_resolved_out_type` CHECK (
              `ResolvedOutType` IS NULL OR `ResolvedOutType` in (_utf8mb4'B',_utf8mb4'D')
          ),
          CONSTRAINT `chk_stock_entry_det_resolution_version` CHECK (`ResolutionVersion` >= 0),
          CONSTRAINT `chk_stock_entry_det_resolution_type` CHECK (
              `ResolutionType` IS NULL OR `ResolutionType` in (_utf8mb4'L',_utf8mb4'B',_utf8mb4'D',_utf8mb4'M')
          ),
          CONSTRAINT `chk_stock_entry_det_origin_pair` CHECK (
              (`OriginStockEntryCod` IS NULL AND `OriginItemNumber` IS NULL)
              OR (`OriginStockEntryCod` IS NOT NULL AND `OriginItemNumber` IS NOT NULL)
          ),
          CONSTRAINT `chk_stock_entry_det_status` CHECK (`Status` in (_utf8mb4'A',_utf8mb4'I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Detalle de entradas excepcionales de stock y sus resoluciones';

        SELECT 'Tabla stock_entry_det creada desde cero.' AS Mensaje;
    ELSE
        -- =============================================
        -- CASO: LA TABLA YA EXISTE -> APLICAR ALTERS
        -- =============================================
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'stock_entry_det'
              AND column_name = 'ResolvedInReasonCode'
        ) THEN
            ALTER TABLE `stock_entry_det`
                ADD COLUMN `ResolvedInReasonCode` varchar(64) DEFAULT NULL
                COMMENT 'Ultimo ConfigCod usado para regresar cantidad a disponible. business_config GroupId=11'
                AFTER `UnavailableReasonCode`;
            ALTER TABLE `stock_entry_det`
                ADD KEY `idx_stock_entry_det_resolved_in_reason` (`ResolvedInReasonCode`);
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'stock_entry_det'
              AND column_name = 'ResolvedOutReasonCode'
        ) THEN
            ALTER TABLE `stock_entry_det`
                ADD COLUMN `ResolvedOutReasonCode` varchar(64) DEFAULT NULL
                COMMENT 'Ultimo ConfigCod usado para retirar cantidad definitivamente. business_config GroupId=12'
                AFTER `ResolvedInReasonCode`;
            ALTER TABLE `stock_entry_det`
                ADD KEY `idx_stock_entry_det_resolved_out_reason` (`ResolvedOutReasonCode`);
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'stock_entry_det'
              AND column_name = 'ResolvedOutType'
        ) THEN
            ALTER TABLE `stock_entry_det`
                ADD COLUMN `ResolvedOutType` char(1) DEFAULT NULL
                COMMENT 'Tipo de salida definitiva: B=Baja, D=Destruccion'
                AFTER `ResolvedOutReasonCode`;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'stock_entry_det'
              AND column_name = 'ResolutionVersion'
        ) THEN
            ALTER TABLE `stock_entry_det`
                ADD COLUMN `ResolutionVersion` int NOT NULL DEFAULT '0'
                COMMENT 'Version incremental para identificar cada resolucion parcial en kardex'
                AFTER `ResolvedOutType`;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints
            WHERE table_schema = DATABASE() AND table_name = 'stock_entry_det'
              AND constraint_name = 'chk_stock_entry_det_resolved_out_type'
        ) THEN
            ALTER TABLE `stock_entry_det`
                ADD CONSTRAINT `chk_stock_entry_det_resolved_out_type`
                CHECK (`ResolvedOutType` IS NULL OR `ResolvedOutType` in (_utf8mb4'B',_utf8mb4'D'));
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints
            WHERE table_schema = DATABASE() AND table_name = 'stock_entry_det'
              AND constraint_name = 'chk_stock_entry_det_resolution_version'
        ) THEN
            ALTER TABLE `stock_entry_det`
                ADD CONSTRAINT `chk_stock_entry_det_resolution_version`
                CHECK (`ResolutionVersion` >= 0);
        END IF;

        IF EXISTS (
            SELECT * FROM information_schema.table_constraints
            WHERE table_schema = DATABASE() AND table_name = 'stock_entry_det'
              AND constraint_name = 'fk_stock_entry_det_unavailable_reason'
              AND constraint_type = 'FOREIGN KEY'
        ) THEN
            ALTER TABLE `stock_entry_det`
                DROP FOREIGN KEY `fk_stock_entry_det_unavailable_reason`;
        END IF;

        IF EXISTS (
            SELECT * FROM information_schema.table_constraints
            WHERE table_schema = DATABASE() AND table_name = 'stock_entry_det'
              AND constraint_name = 'fk_stock_entry_det_resolved_in_reason'
              AND constraint_type = 'FOREIGN KEY'
        ) THEN
            ALTER TABLE `stock_entry_det`
                DROP FOREIGN KEY `fk_stock_entry_det_resolved_in_reason`;
        END IF;

        IF EXISTS (
            SELECT * FROM information_schema.table_constraints
            WHERE table_schema = DATABASE() AND table_name = 'stock_entry_det'
              AND constraint_name = 'fk_stock_entry_det_resolved_out_reason'
              AND constraint_type = 'FOREIGN KEY'
        ) THEN
            ALTER TABLE `stock_entry_det`
                DROP FOREIGN KEY `fk_stock_entry_det_resolved_out_reason`;
        END IF;

        IF EXISTS (
            SELECT * FROM information_schema.table_constraints
            WHERE table_schema = DATABASE() AND table_name = 'stock_entry_det'
              AND constraint_name = 'fk_stock_entry_det_resolution_reason'
              AND constraint_type = 'FOREIGN KEY'
        ) THEN
            ALTER TABLE `stock_entry_det`
                DROP FOREIGN KEY `fk_stock_entry_det_resolution_reason`;
        END IF;

        SELECT 'Tabla stock_entry_det verificada y actualizada.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

-- Ejecutar y limpiar
CALL `p_manage_stock_entry_det`();
DROP PROCEDURE `p_manage_stock_entry_det`;
