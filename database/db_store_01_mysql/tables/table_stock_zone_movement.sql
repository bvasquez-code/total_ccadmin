DROP PROCEDURE IF EXISTS `p_manage_stock_zone_movement`;

DELIMITER $$

CREATE PROCEDURE `p_manage_stock_zone_movement`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
    AND table_name = 'stock_zone_movement';

    IF v_table_exists = 0 THEN
        CREATE TABLE `stock_zone_movement` (
          `StockZoneMovementId` bigint NOT NULL AUTO_INCREMENT,
          `OperationCod` varchar(16) NOT NULL COMMENT 'Codigo de operacion origen',
          `ItemNumber` int DEFAULT NULL COMMENT 'Numero de item/secuencia del documento origen',
          `SourceTable` varchar(32) NOT NULL COMMENT 'Tabla origen',
          `ProductCod` varchar(20) NOT NULL COMMENT 'Codigo de producto',
          `Variant` varchar(4) NOT NULL DEFAULT (_utf8mb4'0000') COMMENT 'Codigo de variante',
          `StoreCod` varchar(4) NOT NULL COMMENT 'Codigo de tienda',
          `WarehouseCod` varchar(8) NOT NULL COMMENT 'Codigo de almacen',
          `SourceZone` varchar(16) NOT NULL COMMENT 'Zona origen del stock',
          `TargetZone` varchar(16) NOT NULL COMMENT 'Zona destino del stock',
          `NumStockMoved` int NOT NULL COMMENT 'Cantidad movida',
          `NumPhysicalStockBefore` int NOT NULL DEFAULT '0',
          `NumPhysicalStockAfter` int NOT NULL DEFAULT '0',
          `NumUnavailableStockBefore` int NOT NULL DEFAULT '0',
          `NumUnavailableStockAfter` int NOT NULL DEFAULT '0',
          `NumReservedStockBefore` int NOT NULL DEFAULT '0',
          `NumReservedStockAfter` int NOT NULL DEFAULT '0',
          `NumTotalStockBefore` int NOT NULL DEFAULT '0',
          `NumTotalStockAfter` int NOT NULL DEFAULT '0',
          `LotNumber` varchar(32) DEFAULT NULL COMMENT 'Numero de lote del producto',
          `ExpirationDate` date DEFAULT NULL COMMENT 'Fecha de vencimiento',
          `CreationUser` varchar(16) NOT NULL,
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `ModifyUser` varchar(16) DEFAULT NULL,
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          `Status` char(1) NOT NULL DEFAULT 'A',
          PRIMARY KEY (`StockZoneMovementId`),
          KEY `idx_stock_zone_movement_source` (`SourceTable`,`OperationCod`,`ItemNumber`),
          KEY `idx_stock_zone_movement_product` (`ProductCod`,`Variant`,`StoreCod`,`WarehouseCod`),
          KEY `fk_stock_zone_movement_store` (`StoreCod`),
          KEY `fk_stock_zone_movement_warehouse` (`WarehouseCod`),
          CONSTRAINT `fk_stock_zone_movement_product` FOREIGN KEY (`ProductCod`) REFERENCES `product` (`ProductCod`),
          CONSTRAINT `fk_stock_zone_movement_store` FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`),
          CONSTRAINT `fk_stock_zone_movement_variant` FOREIGN KEY (`ProductCod`, `Variant`) REFERENCES `product_variant` (`ProductCod`, `Variant`),
          CONSTRAINT `fk_stock_zone_movement_warehouse` FOREIGN KEY (`WarehouseCod`) REFERENCES `warehouse` (`WarehouseCod`),
          CONSTRAINT `chk_stock_zone_movement_qty` CHECK (`NumStockMoved` > 0),
          CONSTRAINT `chk_stock_zone_movement_source_zone` CHECK (`SourceZone` in (_utf8mb4'EXTERNAL',_utf8mb4'PHYSICAL',_utf8mb4'UNAVAILABLE',_utf8mb4'RESERVED',_utf8mb4'OUT')),
          CONSTRAINT `chk_stock_zone_movement_target_zone` CHECK (`TargetZone` in (_utf8mb4'EXTERNAL',_utf8mb4'PHYSICAL',_utf8mb4'UNAVAILABLE',_utf8mb4'RESERVED',_utf8mb4'OUT')),
          CONSTRAINT `chk_stock_zone_movement_before_total` CHECK ((`NumPhysicalStockBefore` + `NumUnavailableStockBefore` + `NumReservedStockBefore`) = `NumTotalStockBefore`),
          CONSTRAINT `chk_stock_zone_movement_after_total` CHECK ((`NumPhysicalStockAfter` + `NumUnavailableStockAfter` + `NumReservedStockAfter`) = `NumTotalStockAfter`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla stock_zone_movement creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla stock_zone_movement ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;

END $$

DELIMITER ;

CALL `p_manage_stock_zone_movement`();
DROP PROCEDURE `p_manage_stock_zone_movement`;
