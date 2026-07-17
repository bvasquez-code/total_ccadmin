DROP PROCEDURE IF EXISTS `p_manage_kardex_zone`;

DELIMITER $$

CREATE PROCEDURE `p_manage_kardex_zone`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'kardex_zone';

    IF v_table_exists = 0 THEN
        CREATE TABLE `kardex_zone` (
          `KardexZoneID` bigint NOT NULL AUTO_INCREMENT,
          `OperationCod` varchar(16) NOT NULL COMMENT 'Codigo de operacion origen',
          `ItemNumber` int DEFAULT NULL COMMENT 'Numero de item/secuencia del documento origen',
          `SourceTable` varchar(32) NOT NULL COMMENT 'Tabla origen',
          `MovementEvent` varchar(32) NOT NULL COMMENT 'Etapa de negocio que origina el movimiento',
          `ProductCod` varchar(20) NOT NULL COMMENT 'Codigo de producto',
          `Variant` varchar(4) NOT NULL DEFAULT (_utf8mb4'0000') COMMENT 'Codigo de variante',
          `StoreCod` varchar(4) NOT NULL COMMENT 'Codigo de tienda',
          `WarehouseCod` varchar(8) NOT NULL COMMENT 'Codigo de almacen',
          `ZoneStockMoved` varchar(16) NOT NULL COMMENT 'Zona cuyo saldo fue modificado',
          `TypeOperation` char(1) NOT NULL COMMENT 'Tipo de operacion: R = resta, S = suma',
          `NumStockMoved` int NOT NULL COMMENT 'Cantidad absoluta movida',
          `NumZoneStockBefore` int NOT NULL DEFAULT '0' COMMENT 'Saldo de la zona antes del movimiento',
          `NumZoneStockAfter` int NOT NULL DEFAULT '0' COMMENT 'Saldo de la zona despues del movimiento',
          `LotNumber` varchar(32) DEFAULT NULL COMMENT 'Numero de lote del producto',
          `ExpirationDate` date DEFAULT NULL COMMENT 'Fecha de vencimiento',
          `CreationUser` varchar(16) NOT NULL,
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `ModifyUser` varchar(16) DEFAULT NULL,
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          `Status` char(1) NOT NULL DEFAULT 'A',
          PRIMARY KEY (`KardexZoneID`),
          KEY `idx_kardex_zone_source_item` (`SourceTable`,`OperationCod`,`ItemNumber`,`MovementEvent`),
          KEY `idx_kardex_zone_product` (`ProductCod`,`Variant`,`StoreCod`,`WarehouseCod`,`ZoneStockMoved`,`KardexZoneID`),
          KEY `fk_kardex_zone_store` (`StoreCod`),
          KEY `fk_kardex_zone_warehouse` (`WarehouseCod`),
          CONSTRAINT `fk_kardex_zone_product` FOREIGN KEY (`ProductCod`) REFERENCES `product` (`ProductCod`),
          CONSTRAINT `fk_kardex_zone_store` FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`),
          CONSTRAINT `fk_kardex_zone_variant` FOREIGN KEY (`ProductCod`, `Variant`) REFERENCES `product_variant` (`ProductCod`, `Variant`),
          CONSTRAINT `fk_kardex_zone_warehouse` FOREIGN KEY (`WarehouseCod`) REFERENCES `warehouse` (`WarehouseCod`),
          CONSTRAINT `chk_kardex_zone_qty` CHECK (`NumStockMoved` > 0),
          CONSTRAINT `chk_kardex_zone_before` CHECK (`NumZoneStockBefore` >= 0),
          CONSTRAINT `chk_kardex_zone_after` CHECK (`NumZoneStockAfter` >= 0),
          CONSTRAINT `chk_kardex_zone_balance` CHECK (abs(`NumZoneStockAfter` - `NumZoneStockBefore`) = `NumStockMoved`),
          CONSTRAINT `chk_kardex_zone_type` CHECK (`TypeOperation` in (_utf8mb4'R',_utf8mb4'S')),
          CONSTRAINT `chk_kardex_zone_type_balance` CHECK (
              (`TypeOperation` = _utf8mb4'S' AND `NumZoneStockAfter` > `NumZoneStockBefore`)
              OR (`TypeOperation` = _utf8mb4'R' AND `NumZoneStockAfter` < `NumZoneStockBefore`)
          )
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla kardex_zone creada desde cero.' AS Mensaje;
    ELSE
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'kardex_zone'
              AND column_name = 'TypeOperation'
        ) THEN
            ALTER TABLE `kardex_zone`
                ADD COLUMN `TypeOperation` char(1) DEFAULT NULL
                COMMENT 'Tipo de operacion: R = resta, S = suma' AFTER `ZoneStockMoved`;
            UPDATE `kardex_zone`
            SET `TypeOperation` = CASE
                WHEN `NumZoneStockAfter` > `NumZoneStockBefore` THEN 'S'
                ELSE 'R'
            END;
            ALTER TABLE `kardex_zone`
                MODIFY COLUMN `TypeOperation` char(1) NOT NULL
                COMMENT 'Tipo de operacion: R = resta, S = suma';
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'kardex_zone'
              AND column_name = 'MovementEvent'
        ) THEN
            ALTER TABLE `kardex_zone`
                ADD COLUMN `MovementEvent` varchar(32) NOT NULL DEFAULT 'LEGACY'
                COMMENT 'Etapa de negocio que origina el movimiento' AFTER `SourceTable`;
            ALTER TABLE `kardex_zone` ALTER COLUMN `MovementEvent` DROP DEFAULT;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints
            WHERE table_schema = DATABASE()
              AND table_name = 'kardex_zone'
              AND constraint_name = 'chk_kardex_zone_type'
        ) THEN
            ALTER TABLE `kardex_zone`
                ADD CONSTRAINT `chk_kardex_zone_type`
                CHECK (`TypeOperation` in (_utf8mb4'R',_utf8mb4'S'));
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints
            WHERE table_schema = DATABASE()
              AND table_name = 'kardex_zone'
              AND constraint_name = 'chk_kardex_zone_type_balance'
        ) THEN
            ALTER TABLE `kardex_zone`
                ADD CONSTRAINT `chk_kardex_zone_type_balance`
                CHECK (
                    (`TypeOperation` = _utf8mb4'S' AND `NumZoneStockAfter` > `NumZoneStockBefore`)
                    OR (`TypeOperation` = _utf8mb4'R' AND `NumZoneStockAfter` < `NumZoneStockBefore`)
                );
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'kardex_zone'
              AND index_name = 'idx_kardex_zone_source_item'
              AND column_name = 'MovementEvent'
        ) THEN
            IF EXISTS (
                SELECT * FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'kardex_zone'
                  AND index_name = 'idx_kardex_zone_source_item'
            ) THEN
                ALTER TABLE `kardex_zone` DROP INDEX `idx_kardex_zone_source_item`;
            END IF;
            ALTER TABLE `kardex_zone`
                ADD KEY `idx_kardex_zone_source_item` (`SourceTable`,`OperationCod`,`ItemNumber`,`MovementEvent`);
        END IF;

        SELECT 'Tabla kardex_zone verificada y actualizada.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_kardex_zone`();
DROP PROCEDURE `p_manage_kardex_zone`;
