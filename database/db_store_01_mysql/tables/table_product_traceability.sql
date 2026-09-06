DROP PROCEDURE IF EXISTS `p_manage_product_traceability`;

DELIMITER $$

CREATE PROCEDURE `p_manage_product_traceability`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'product_traceability';

    IF v_table_exists = 0 THEN
        CREATE TABLE `product_traceability` (
          `ProductTraceabilityID` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno del registro de trazabilidad',
          `TechnicalLot` varchar(20) NOT NULL COMMENT 'Lote tecnico global con prefijo LT que identifica el origen economico de las unidades',
          `KardexID` bigint NOT NULL COMMENT 'Movimiento de kardex que genero esta asignacion',
          `AllocationNumber` int NOT NULL COMMENT 'Correlativo de la particion FIFO dentro del movimiento de kardex',
          `OriginProductTraceabilityID` bigint DEFAULT NULL COMMENT 'Registro de salida del cual proviene una transferencia o devolucion',
          `OperationCod` varchar(16) NOT NULL COMMENT 'Codigo del documento de negocio que produjo el movimiento',
          `ItemNumber` int DEFAULT NULL COMMENT 'Numero de item del documento de negocio',
          `SourceTable` varchar(32) NOT NULL COMMENT 'Tabla cabecera que identifica el tipo de documento origen',
          `TypeOperation` char(1) NOT NULL COMMENT 'Tipo de movimiento: S=Entrada, R=Salida',
          `ProductCod` varchar(20) NOT NULL COMMENT 'Codigo del producto',
          `Variant` varchar(4) NOT NULL DEFAULT (_utf8mb4'0000') COMMENT 'Codigo de variante del producto',
          `StoreCod` varchar(4) NOT NULL COMMENT 'Codigo del local afectado',
          `WarehouseCod` varchar(8) NOT NULL COMMENT 'Codigo del almacen afectado',
          `LotNumber` varchar(32) DEFAULT NULL COMMENT 'Lote fisico informado por el movimiento, independiente del lote tecnico',
          `ExpirationDate` date DEFAULT NULL COMMENT 'Fecha de vencimiento fisica informada, si aplica',
          `NumUnit` int NOT NULL COMMENT 'Cantidad asignada a este registro o particion FIFO',
          `NumUnitAvailable` int NOT NULL DEFAULT '0' COMMENT 'Cantidad de entrada que aun puede ser consumida',
          `NumUnitPriceCost` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'Costo unitario heredado por el lote tecnico',
          `NumTotalPriceCost` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'Costo total de las unidades de este registro',
          `NumUnitPriceSale` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'Precio unitario de venta cuando el documento lo informa',
          `NumTotalPriceSale` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'Precio total de venta de las unidades de este registro',
          `OperationDate` datetime NOT NULL COMMENT 'Fecha y hora efectiva del movimiento de kardex',
          `AvailabilityStatus` char(1) NOT NULL COMMENT 'Disponibilidad: A=Disponible, E=Agotado, N=No aplica para salidas',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que modifico por ultima vez',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico: A=Activo, I=Inactivo',
          PRIMARY KEY (`ProductTraceabilityID`),
          UNIQUE KEY `uq_product_traceability_kardex_allocation` (`KardexID`,`AllocationNumber`),
          KEY `idx_product_traceability_fifo` (`StoreCod`,`WarehouseCod`,`ProductCod`,`Variant`,`AvailabilityStatus`,`ProductTraceabilityID`),
          KEY `idx_product_traceability_technical_lot` (`TechnicalLot`),
          KEY `idx_product_traceability_source` (`SourceTable`,`OperationCod`,`StoreCod`,`ItemNumber`),
          KEY `idx_product_traceability_origin` (`OriginProductTraceabilityID`),
          KEY `idx_product_traceability_variant` (`ProductCod`,`Variant`),
          CONSTRAINT `fk_product_traceability_kardex` FOREIGN KEY (`KardexID`) REFERENCES `kardex` (`kardexID`),
          CONSTRAINT `fk_product_traceability_origin` FOREIGN KEY (`OriginProductTraceabilityID`) REFERENCES `product_traceability` (`ProductTraceabilityID`),
          CONSTRAINT `fk_product_traceability_variant` FOREIGN KEY (`ProductCod`,`Variant`) REFERENCES `product_variant` (`ProductCod`,`Variant`),
          CONSTRAINT `fk_product_traceability_store` FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`),
          CONSTRAINT `fk_product_traceability_warehouse` FOREIGN KEY (`WarehouseCod`) REFERENCES `warehouse` (`WarehouseCod`),
          CONSTRAINT `chk_product_traceability_type` CHECK (`TypeOperation` in (_utf8mb4'S',_utf8mb4'R')),
          CONSTRAINT `chk_product_traceability_quantity` CHECK (`NumUnit` > 0 AND `NumUnitAvailable` >= 0 AND `NumUnitAvailable` <= `NumUnit`),
          CONSTRAINT `chk_product_traceability_prices` CHECK (`NumUnitPriceCost` >= 0 AND `NumTotalPriceCost` >= 0 AND `NumUnitPriceSale` >= 0 AND `NumTotalPriceSale` >= 0),
          CONSTRAINT `chk_product_traceability_availability` CHECK (
              (`TypeOperation` = _utf8mb4'S' AND (
                  (`NumUnitAvailable` > 0 AND `AvailabilityStatus` = _utf8mb4'A')
                  OR (`NumUnitAvailable` = 0 AND `AvailabilityStatus` = _utf8mb4'E')
              ))
              OR (`TypeOperation` = _utf8mb4'R' AND `NumUnitAvailable` = 0 AND `AvailabilityStatus` = _utf8mb4'N')
          ),
          CONSTRAINT `chk_product_traceability_status` CHECK (`Status` in (_utf8mb4'A',_utf8mb4'I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Identidad economica y consumo FIFO de las unidades de producto';

        SELECT 'Tabla product_traceability creada desde cero.' AS Mensaje;
    ELSE
        IF EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'product_traceability'
              AND column_name = 'TechnicalLot'
              AND character_maximum_length <> 20
        ) THEN
            ALTER TABLE `product_traceability`
                MODIFY COLUMN `TechnicalLot` varchar(20) NOT NULL
                COMMENT 'Lote tecnico global con prefijo LT que identifica el origen economico de las unidades';
        END IF;

        SELECT 'Tabla product_traceability ya existe.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_product_traceability`();
DROP PROCEDURE `p_manage_product_traceability`;
